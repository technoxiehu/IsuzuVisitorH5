<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useVisitorStore } from '@/stores/visitor'
import { deleteApplication, getApplicationList, getUser } from '@/api/visitor'
import { formatDateTime } from '@/utils/date'
import { toAvatarUrl } from '@/utils/avatar'
import { maskIdCard } from '@/utils/mask'
import CompanionList from '@/components/CompanionList.vue'

defineOptions({ name: 'VisitorListView' })

// 访客预约列表页（PRD §5.5）：访客记录页 + 门卫核验凭证
const route = useRoute()
const router = useRouter()
const store = useVisitorStore()

const loading = ref(false)

// 当前时间戳（防伪措施，PRD v1.3：前端本地时间，每秒刷新）
const nowTime = ref(formatDateTime(new Date(), true))
let timer = null

// 列表数据轮询（门卫核验/访客查看需及时反映审批结果，15 秒静默刷新）
const REFRESH_INTERVAL = 15000
let refreshTimer = null
let refreshing = false

onMounted(() => {
  timer = setInterval(() => {
    nowTime.value = formatDateTime(new Date(), true)
  }, 1000)
  loadData()
  startRefresh()
  document.addEventListener('visibilitychange', onVisibilityChange)
  // 入口拦截弹窗（当日拒绝数 ≥3）
  if (route.query.toast === 'blocked') {
    showToast('审批人拒绝近期访问，谢谢。')
  }
})
onUnmounted(() => {
  clearInterval(timer)
  clearInterval(refreshTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})

// 页面切后台时暂停轮询（定时器会被节流且请求无效），恢复前台立即刷新并重启
function onVisibilityChange() {
  if (document.hidden) {
    clearInterval(refreshTimer)
    refreshTimer = null
  } else if (!refreshTimer) {
    loadData({ silent: true })
    startRefresh()
  }
}

function startRefresh() {
  refreshTimer = setInterval(() => loadData({ silent: true }), REFRESH_INTERVAL)
}

async function loadData({ silent = false } = {}) {
  // 防重：上一轮请求未完成时跳过（轮询与首次加载/恢复刷新可能重叠）
  if (refreshing) return
  if (!silent) loading.value = true
  refreshing = true
  try {
    if (!store.userInfo) {
      const res = await getUser(store.visitorId)
      if (res.data?.user) {
        store.setUserInfo(res.data.user)
      }
    }
    const listRes = await getApplicationList(store.visitorId)
    store.setApplicationData(listRes.data || {})
  } catch {
    // 拦截器已提示
  } finally {
    refreshing = false
    if (!silent) loading.value = false
  }
}

const statusMap = computed(() => ({
  0: { text: '未审批', type: 'primary' },
  1: { text: '通过', type: 'success' },
  2: { text: '拒绝', type: 'danger' },
}))

function isExpired(record) {
  return new Date(record.endTime.replace(' ', 'T')) < new Date()
}

// 记录访问截止后一律显示「完成」（已完结，门卫不再放行）
function statusInfo(record) {
  if (isExpired(record)) {
    return { text: '完成', type: 'default' }
  }
  return statusMap.value[record.status]
}

function onProfileClick() {
  router.push('/user-info?mode=edit')
}

// 左滑删除待审批申请单（PRD v1.9）：二次确认后逻辑删除，成功后本地移除并即时刷新
async function onDelete(record) {
  try {
    await showConfirmDialog({
      title: '删除申请单',
      message: '确定删除这条待审批的访问申请吗？删除后需重新提交申请。',
    })
  } catch {
    return // 用户取消
  }
  try {
    await deleteApplication(store.visitorId, record.applicationId)
    showToast('删除成功')
    store.records = store.records.filter((r) => r.applicationId !== record.applicationId)
  } catch {
    // 拦截器已提示（如该单已审批无法删除）
  }
}

// 非有效记录的副文案（PRD v1.5：列表展示全部记录，门卫仅认「通过且有效期内」）
function effectiveTip(record) {
  const now = new Date()
  if (record.effective === false) {
    if (new Date(record.startTime.replace(' ', 'T')) > now) return '未到访问时间'
    if (new Date(record.endTime.replace(' ', 'T')) < now) return '已过期'
  }
  return ''
}
</script>

<template>
  <div class="page list-page">
    <!-- 当前时间戳（防截图冒用，PRD v1.3 §5.5） -->
    <div class="timestamp">当前时间：{{ nowTime }}</div>

    <h2 class="page-title">访问预约记录</h2>

    <!-- 有效审批记录列表 -->
    <van-loading v-if="loading" size="24px" class="list-loading" />
    <template v-else>
      <van-empty v-if="!store.records.length" image="search" description=" " />
      <div v-else class="record-list">
        <van-swipe-cell v-for="record in store.records" :key="record.applicationId">
          <div class="record-card" :class="{ 'record-inactive': record.effective === false }">
            <div class="record-main">
              <div class="record-host">被访人：{{ record.hostName }}</div>
              <div class="record-time">访问截止：{{ formatDateTime(new Date(record.endTime.replace(' ', 'T'))) }}
                <span v-if="record.effective === false" class="record-tip">{{ effectiveTip(record) }}</span>
              </div>
              <!-- 随行人员名单（门卫核验用，PRD v1.4 §5.5；老数据无名单时跳过） -->
              <div v-if="record.companions?.length" class="record-companions">
                <CompanionList :companions="record.companions" />
              </div>
            </div>
            <van-tag :type="statusInfo(record)?.type" round>
              {{ statusInfo(record)?.text }}
            </van-tag>
          </div>
          <!-- 仅待审批记录可左滑删除（PRD v1.9） -->
          <template #right v-if="record.status === '0'">
            <van-button square type="danger" class="record-delete" @click="onDelete(record)">
              删除
            </van-button>
          </template>
        </van-swipe-cell>
      </div>
    </template>

    <!-- 访客信息（固定屏幕底部吸底栏，我的信息修改入口，PRD §5.5） -->
    <div class="profile-fixed">
      <div class="profile-section-title">访客信息</div>
      <div class="profile-card" @click="onProfileClick">
        <van-image round width="64" height="64" :src="toAvatarUrl(store.userInfo?.avatar)" fit="cover">
          <template #error><van-icon name="user-o" size="40" /></template>
        </van-image>
        <div class="profile-info">
          <div class="profile-name">{{ store.userInfo?.name || '—' }}</div>
          <div class="profile-company">{{ store.userInfo?.company || '' }}</div>
          <!-- 身份证号（脱敏，门卫对照实体证件核验；后端已脱敏，maskIdCard 幂等） -->
          <div class="profile-idcard">身份证：{{ store.userInfo?.idCard ? maskIdCard(store.userInfo.idCard) : '—' }}</div>
          <!-- 车牌号（非必填，门卫核验车辆；无则隐藏） -->
          <div v-if="store.userInfo?.plateNo" class="profile-idcard">车牌号：{{ store.userInfo.plateNo }}</div>
        </div>
        <van-icon name="arrow" color="#999" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 页面底部留白，避免吸底栏遮挡最后一条记录 */
.list-page {
  padding-bottom: 160px;
}

/* 访客信息吸底栏（固定屏幕底部，我的信息修改入口） */
.profile-fixed {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 16px calc(env(safe-area-inset-bottom, 0px) + 12px);
  background: var(--page-bg);
  border-top: 1px solid var(--color-border);
}

.profile-section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

/* 访客信息卡片（蓝色细边框，区别于普通记录卡片） */
.profile-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--card-bg);
  border: 1px solid #4a90e2;
  border-radius: var(--radius-lg);
  padding: 14px 16px;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.profile-company {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.profile-idcard {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}

.timestamp {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  font-variant-numeric: tabular-nums;
}

.list-loading {
  display: block;
  margin: 40px auto;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.record-host {
  font-size: 15px;
  color: var(--color-text);
}

.record-time {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.record-tip {
  margin-left: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.record-inactive {
  opacity: 0.55;
}

.record-delete {
  height: 100%;
  width: 72px;
}

.record-companions {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--color-border);
}
</style>
