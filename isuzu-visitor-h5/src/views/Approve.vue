<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { approve, getApproveDetail } from '@/api/visitor'
import { toAvatarUrl } from '@/utils/avatar'
import CompanionList from '@/components/CompanionList.vue'

defineOptions({ name: 'ApproveView' })

// 审批确认页（被访问人侧，PRD §5.7）：邮件链接 token 进入
// 只能审批一次：601 已审批直接拦截，不渲染审批操作区（PRD v1.3）
const route = useRoute()
const router = useRouter()

// 状态：loading 加载中 / ready 可审批 / done 已完成审批 / revoked 申请单已撤销 / invalid 链接无效 / finished 审批完成
const pageState = ref('loading')
const detail = ref(null)
const submitting = ref(false)

onMounted(async () => {
  // 等待路由解析完成，避免初始导航未就绪时 query 为空
  await router.isReady()
  const token = route.query.token
  if (!token) {
    pageState.value = 'invalid'
    return
  }
  try {
    const res = await getApproveDetail(token)
    detail.value = res.data.application
    pageState.value = 'ready'
  } catch (err) {
    // 601 已完成审批 / 602 申请单已撤销 / 401 链接无效（拦截器已 Toast 提示）
    if (err?.code === 601) pageState.value = 'done'
    else if (err?.code === 602) pageState.value = 'revoked'
    else pageState.value = 'invalid'
  }
})

/** 点击批准/拒绝直接回写（无二次确认弹窗，PRD v1.3 §5.7） */
async function onApprove(action) {
  submitting.value = true
  try {
    await approve(route.query.token, action)
    pageState.value = 'finished'
  } catch {
    // 重复审批 601：回到已审批态
    if (pageState.value === 'ready') {
      pageState.value = 'done'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-loading v-if="pageState === 'loading'" size="32px" class="state-loading" />

    <!-- 链接无效 -->
    <van-empty v-else-if="pageState === 'invalid'" image="error" description="链接无效或已过期" />

    <!-- 已审批：不渲染审批操作区 -->
    <van-empty v-else-if="pageState === 'done'" image="error" description="该申请单已完成审批" />

    <!-- 访客已撤销申请单 -->
    <van-empty v-else-if="pageState === 'revoked'" image="error" description="该申请单已撤销" />

    <!-- 审批完成 -->
    <div v-else-if="pageState === 'finished'" class="state-finished">
      <van-icon name="checked" size="56px" color="#07c160" />
      <h3 class="finished-title">审批完成</h3>
    </div>

    <!-- 可审批：申请详情 + 操作区 -->
    <template v-else-if="detail">
      <h2 class="page-title">访客预约审批</h2>
      <div class="page-card">
        <!-- 申请人信息（头像居中醒目展示，帮助被访问人确认身份） -->
        <div class="visitor-header">
          <van-image round width="120" height="120" :src="toAvatarUrl(detail.visitorAvatar)" fit="cover">
            <template #error><van-icon name="user-o" size="72" /></template>
          </van-image>
          <div class="visitor-name">{{ detail.visitorName || '—' }}</div>
          <div class="visitor-company">{{ detail.visitorCompany || '' }}</div>
        </div>
        <van-cell-group inset>
          <van-cell title="被访问人" :value="detail.hostName || '—'" />
          <van-cell title="开始日期" :value="(detail.startTime || '').slice(0, 10)" />
          <van-cell title="结束日期" :value="(detail.endTime || '').slice(0, 10)" />
          <van-cell title="访问事由" :value="detail.reason" />
          <van-cell v-if="detail.remark" title="备注" :value="detail.remark" />
        </van-cell-group>
        <!-- 随行人员名单（PRD v1.4/v1.10 §5.7：固定显示标题与名单/空态；接口已保证脱敏，此处组件内再兜底） -->
        <CompanionList class="approve-companions" :companions="detail.companions || []" />
      </div>
      <div class="action-row">
        <van-button type="danger" block round :loading="submitting" @click="onApprove('reject')">
          拒绝
        </van-button>
        <van-button type="success" block round :loading="submitting" @click="onApprove('approve')">
          同意
        </van-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.state-loading {
  display: block;
  margin: 80px auto;
}

.state-finished {
  text-align: center;
  padding-top: 80px;
}

.finished-title {
  margin-top: 12px;
  color: var(--color-text);
}

.visitor-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 8px 0 16px;
}

.visitor-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin-top: 12px;
}

.visitor-company {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.approve-companions {
  margin-top: 16px;
  padding: 16px 24px 16px;
  /* 虚横线分隔随行人员区块与上方访问事由（与列表页 .record-companions 同款样式） */
  border-top: 1px dashed var(--color-border);
}
</style>
