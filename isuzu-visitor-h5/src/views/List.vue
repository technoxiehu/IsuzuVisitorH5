<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useVisitorStore } from '@/stores/visitor'
import { getApplicationList, getUser } from '@/api/visitor'
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
onMounted(() => {
  timer = setInterval(() => {
    nowTime.value = formatDateTime(new Date(), true)
  }, 1000)
  loadData()
  // 入口拦截弹窗（当日拒绝数 ≥3）
  if (route.query.toast === 'blocked') {
    showToast('审批人拒绝近期访问，谢谢。')
  }
})
onUnmounted(() => clearInterval(timer))

async function loadData() {
  loading.value = true
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
    loading.value = false
  }
}

const statusMap = computed(() => ({
  0: { text: '未审批', type: 'primary' },
  1: { text: '通过', type: 'success' },
  2: { text: '拒绝', type: 'danger' },
}))

function onProfileClick() {
  router.push('/user-info?mode=edit')
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
  <div class="page">
    <!-- 顶部用户信息区：门卫核验身份依据，醒目展示防冒用（PRD §5.5） -->
    <div class="profile-card" @click="onProfileClick">
      <van-image round width="64" height="64" :src="toAvatarUrl(store.userInfo?.avatar)" fit="cover">
        <template #error><van-icon name="user-o" size="40" /></template>
      </van-image>
      <div class="profile-info">
        <div class="profile-name">{{ store.userInfo?.name || '—' }}</div>
        <div class="profile-company">{{ store.userInfo?.company || '' }}</div>
        <!-- 身份证号（脱敏，门卫对照实体证件核验；后端已脱敏，maskIdCard 幂等） -->
        <div class="profile-idcard">身份证：{{ store.userInfo?.idCard ? maskIdCard(store.userInfo.idCard) : '—' }}</div>
      </div>
      <van-icon name="arrow" color="#999" />
    </div>

    <!-- 当前时间戳（防截图冒用，PRD v1.3 §5.5） -->
    <div class="timestamp">当前时间：{{ nowTime }}</div>

    <h2 class="page-title">访问预约记录</h2>

    <!-- 有效审批记录列表 -->
    <van-loading v-if="loading" size="24px" class="list-loading" />
    <template v-else>
      <van-empty v-if="!store.records.length" image="search" description=" " />
      <div v-else class="record-list">
        <div v-for="record in store.records" :key="record.applicationId" class="record-card"
          :class="{ 'record-inactive': record.effective === false }">
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
          <van-tag :type="statusMap[record.status]?.type" round>
            {{ statusMap[record.status]?.text }}
          </van-tag>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.profile-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 8px;
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

.record-companions {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--color-border);
}
</style>
