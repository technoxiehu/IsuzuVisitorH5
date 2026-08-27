<template>
  <div class="guard-wall" :class="{ 'guard-wall--full': fullscreen }">
    <!-- 工具栏：搜索 + 统计 + 自动刷新状态 -->
    <div class="guard-wall__toolbar">
      <div class="guard-wall__search">
        <el-input
          v-model="keyword"
          :placeholder="fullscreen ? '搜索姓名 / 手机号 / 身份证号 / 车牌号' : '姓名 / 手机号 / 身份证号 / 车牌号'"
          clearable
          class="guard-wall__search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
      </div>
      <div class="guard-wall__meta">
        <span class="guard-wall__count">共 {{ cards.length }} 张有效单据</span>
        <span class="guard-wall__refresh" :class="{ 'is-refreshing': refreshing }">
          <el-icon v-if="refreshing" class="is-loading"><Refresh /></el-icon>
          <el-icon v-else><RefreshRight /></el-icon>
          {{ countdown }} 秒后自动刷新
        </span>
      </div>
    </div>

    <!-- 卡片墙 -->
    <div v-loading="loading" class="guard-wall__grid">
      <template v-if="cards.length">
        <div
          v-for="card in cards"
          :key="card.applicationId"
          class="guard-card"
          :class="{ 'guard-card--released': card.entryCount > 0 }"
        >
          <!-- 卡片头：头像 + 姓名/单位 + 状态 -->
          <div class="guard-card__head">
            <el-avatar :size="fullscreen ? 64 : 56" :src="avatarUrl(card.visitorAvatar)" class="guard-card__avatar">
              {{ (card.visitorName || '访').slice(0, 1) }}
            </el-avatar>
            <div class="guard-card__who">
              <div class="guard-card__name">{{ card.visitorName || '-' }}</div>
              <div class="guard-card__company">{{ card.visitorCompany || '-' }}</div>
              <el-tag v-if="card.plateNo" size="small" effect="plain" class="guard-card__plate">{{ card.plateNo }}</el-tag>
            </div>
            <el-tag
              :type="card.entryCount > 0 ? 'success' : 'primary'"
              effect="light"
              round
              class="guard-card__status"
            >
              {{ card.entryCount > 0 ? `已放行 ${card.entryCount} 次` : '待放行' }}
            </el-tag>
          </div>

          <!-- 卡片主体：核验信息 -->
          <el-descriptions :column="1" size="small" class="guard-card__body">
            <el-descriptions-item label="手机号">{{ card.visitorPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{ card.visitorIdCard || '-' }}</el-descriptions-item>
            <el-descriptions-item label="被访人">{{ card.hostName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="访问事由">{{ card.reason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="访问时间">{{ formatRange(card.startTime, card.endTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="card.companions && card.companions.length" label="随行人员">
              <span class="guard-card__companions">{{ companionText(card.companions) }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 卡片脚：放行记录 + 放行按钮 -->
          <div class="guard-card__foot">
            <span class="guard-card__last">
              {{ card.lastEntryTime ? `最近放行 ${card.lastEntryTime}` : '尚未放行' }}
            </span>
            <el-button
              v-hasPermi="['visitor:guard:entry']"
              type="primary"
              :plain="card.entryCount > 0"
              :loading="releasingId === card.applicationId"
              class="guard-card__release"
              @click="handleRelease(card)"
            >
              {{ card.entryCount > 0 ? '再次放行' : '放行' }}
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-else-if="!loading" :description="fullscreen ? '当前时段暂无有效来访单据' : '暂无有效来访单据'" />
    </div>
  </div>
</template>

<script setup>
import { listGuardCard, createGuardEntry } from '@/api/visitor/guard'

defineProps({
  fullscreen: { type: Boolean, default: false }
})

const { proxy } = getCurrentInstance()

const REFRESH_INTERVAL = 10 // 自动刷新间隔（秒）

const cards = ref([])
const loading = ref(false)
const refreshing = ref(false)
const keyword = ref('')
const countdown = ref(REFRESH_INTERVAL)
const releasingId = ref('')

let refreshTimer = null
let tickTimer = null

/** 头像：相对路径 /profile/... 需拼 baseURL 走代理到后端静态资源 */
function avatarUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//.test(path)) return path
  return (import.meta.env.VITE_APP_BASE_API || '') + path
}

/** 访问时间范围展示：同日显示「MM-DD HH:mm ~ HH:mm」，跨日显示完整 */
function formatRange(start, end) {
  if (!start && !end) return '-'
  const s = start ? start.slice(0, 16) : ''
  const e = end ? end.slice(0, 16) : ''
  if (s && e && s.slice(0, 10) === e.slice(0, 10)) {
    return `${s.slice(5)} ~ ${e.slice(11)}`
  }
  return `${s} ~ ${e}`
}

/** 随行人员名单：姓名(脱敏证) 分号连接 */
function companionText(list) {
  return list.map((c) => `${c.name}(${c.idCard})`).join('；')
}

/** 拉取卡片：silent=true 静默刷新（不闪 loading） */
async function getList(silent = false) {
  if (silent) {
    refreshing.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await listGuardCard({
      pageNum: 1,
      pageSize: 200,
      keyword: keyword.value || undefined
    })
    cards.value = (res && res.rows) || []
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function handleSearch() {
  countdown.value = REFRESH_INTERVAL
  getList(false)
}

/** 放行：无二次确认、不限次数；成功后静默刷新以更新「已放行」标记 */
async function handleRelease(card) {
  releasingId.value = card.applicationId
  try {
    await createGuardEntry({ applicationId: card.applicationId })
    proxy.$modal.msgSuccess('放行成功')
  } catch (e) {
    // 601/500 等错误已由 request 拦截器统一提示，此处静默
  } finally {
    releasingId.value = ''
    getList(true)
  }
}

onMounted(() => {
  getList(false)
  refreshTimer = setInterval(() => getList(true), REFRESH_INTERVAL * 1000)
  tickTimer = setInterval(() => {
    countdown.value = countdown.value <= 1 ? REFRESH_INTERVAL : countdown.value - 1
  }, 1000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  clearInterval(tickTimer)
})

// 供全屏页通过 ref 触发手动刷新
defineExpose({ getList })
</script>

<style scoped>
.guard-wall {
  width: 100%;
}

/* ---------- 工具栏 ---------- */
.guard-wall__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.guard-wall__search {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1 1 380px;
  max-width: 560px;
}

.guard-wall--full .guard-wall__search-input :deep(.el-input__inner) {
  height: 44px;
  font-size: 16px;
}

.guard-wall__meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.guard-wall__count {
  font-weight: 600;
  color: #303133;
}

.guard-wall__refresh {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #909399;
}

.guard-wall__refresh.is-refreshing {
  color: var(--el-color-primary);
}

/* ---------- 卡片网格 ---------- */
.guard-wall__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  min-height: 200px;
}

.guard-wall--full .guard-wall__grid {
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

/* ---------- 卡片 ---------- */
.guard-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-left: 4px solid var(--el-color-primary);
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.guard-card:hover {
  box-shadow: 0 6px 18px rgba(0, 21, 41, 0.12);
  transform: translateY(-2px);
}

/* 已放行：绿色左描边 + 浅绿底角标 */
.guard-card--released {
  border-left-color: var(--el-color-success);
  background: linear-gradient(180deg, #ffffff 0%, #f8fbf7 100%);
}

.guard-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.guard-card__avatar {
  flex-shrink: 0;
  background: #ecf5ff;
  color: var(--el-color-primary);
  font-size: 22px;
  font-weight: 600;
}

.guard-card__who {
  flex: 1;
  min-width: 0;
}

.guard-card__name {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guard-card__company {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guard-card__plate {
  margin-top: 4px;
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.guard-card__status {
  flex-shrink: 0;
  font-weight: 600;
}

.guard-card__body {
  margin-top: 12px;
}

.guard-card__body :deep(.el-descriptions__label) {
  color: #909399;
  width: 74px;
}

.guard-card__companions {
  color: #606266;
  line-height: 1.6;
  word-break: break-all;
}

.guard-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.guard-card__last {
  font-size: 12px;
  color: #909399;
}

.guard-card__release {
  flex-shrink: 0;
}

.guard-card--released .guard-card__release {
  font-weight: 600;
}
</style>
