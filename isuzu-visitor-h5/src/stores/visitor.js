import { ref } from 'vue'
import { defineStore } from 'pinia'
import { generateUUID } from '@/utils/uuid'

const VISITOR_ID_KEY = 'visitor_id'

// 访客状态（visitor_id 持久化 localStorage；用户信息与审批记录随会话刷新）
export const useVisitorStore = defineStore('visitor', () => {
  const visitorId = ref(localStorage.getItem(VISITOR_ID_KEY) || '')
  const userInfo = ref(null)
  const todayRejectCount = ref(0)
  const records = ref([])

  /** 确保 visitor_id 存在（无则生成 UUID 并存储），PRD §5.1 */
  function ensureVisitorId() {
    if (!visitorId.value) {
      visitorId.value = generateUUID()
      localStorage.setItem(VISITOR_ID_KEY, visitorId.value)
    }
    return visitorId.value
  }

  function setUserInfo(info) {
    userInfo.value = info
  }

  /** 更新审批记录与当日拒绝数 */
  function setApplicationData(data) {
    todayRejectCount.value = data.todayRejectCount ?? 0
    records.value = data.records || []
  }

  return { visitorId, userInfo, todayRejectCount, records, ensureVisitorId, setUserInfo, setApplicationData }
})
