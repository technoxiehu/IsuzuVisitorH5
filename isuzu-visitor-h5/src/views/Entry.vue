<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useVisitorStore } from '@/stores/visitor'
import { getUser, getApplicationList } from '@/api/visitor'

defineOptions({ name: 'EntryView' })

// 入口分流（PRD §4.1 主流程、§5.1 新老用户判断与分流规则）
const router = useRouter()
const store = useVisitorStore()

onMounted(async () => {
  // 等待路由解析完成，保证初始导航就绪后再分流跳转
  await router.isReady()
  // 进入前是否已有 visitor_id（无则为新用户）
  const isNew = !store.visitorId
  const visitorId = store.ensureVisitorId()

  if (isNew) {
    router.replace('/user-info')
    return
  }

  try {
    // 老用户判断：后台按 visitor_id 查询
    const res = await getUser(visitorId)
    const user = res.data?.user
    if (!user) {
      // 后台查无，视为新用户
      router.replace('/user-info')
      return
    }
    store.setUserInfo(user)

    // 老用户分流：按有效审批记录与当日拒绝数
    const listRes = await getApplicationList(visitorId)
    const { todayRejectCount, records } = listRes.data || {}
    store.setApplicationData({ todayRejectCount, records })

    // 入口拦截（PRD §7）：当日拒绝数达 3 次，禁止再申请
    if (todayRejectCount >= 3) {
      router.replace('/list?toast=blocked')
      return
    }
    // 有效记录：当前时刻落在访问窗口内（effective 由后端按应用服务器时间计算，PRD v1.5）
    const effectiveRecords = (records || []).filter((r) => r.effective !== false)
    const hasPendingOrApproved = effectiveRecords.some((r) => r.status === '0' || r.status === '1')
    if (effectiveRecords.length === 0) {
      router.replace('/application')
    } else if (hasPendingOrApproved) {
      router.replace('/list')
    } else {
      // 有效记录全为拒绝且当日拒绝数 < 3：可再次申请，弹窗提醒剩余次数
      const remain = 3 - todayRejectCount
      router.replace(`/application?toast=rejectRemain&remain=${remain}`)
    }
  } catch {
    // 网络异常等：拦截器已提示，停留本页可刷新重试
  }
})
</script>

<template>
  <div class="page entry-page">
    <van-loading size="32px" vertical>正在进入...</van-loading>
  </div>
</template>

<style scoped>
.entry-page {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
}
</style>
