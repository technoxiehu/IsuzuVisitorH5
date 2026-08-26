<template>
  <div class="app-container guard-page">
    <div class="guard-page__bar">
      <div class="guard-page__title">
        <span class="guard-page__dot"></span>
        来访核验
        <span class="guard-page__sub">今日有效审批单据 · 每 10 秒自动刷新</span>
      </div>
      <el-button type="primary" plain icon="FullScreen" @click="openFullscreen">全屏大屏</el-button>
    </div>

    <GuardCardWall />
  </div>
</template>

<script setup name="Guard">
import { useRouter } from 'vue-router'
import GuardCardWall from './components/GuardCardWall.vue'

const router = useRouter()

/** 全屏大屏：在本次用户手势内请求浏览器原生全屏（100% 生效，真正的全屏、无地址栏/标题栏/标签栏），
 *  随后切换到独立大屏路由（无后台框架）；若被浏览器拦截则静默以窗口模式展示 */
function openFullscreen() {
  const el = document.documentElement
  const req = el.requestFullscreen || el.webkitRequestFullscreen || el.msRequestFullscreen
  if (req) {
    const p = req.call(el)
    if (p && typeof p.catch === 'function') {
      p.catch(() => {})
    }
  }
  router.push('/visitor/guard/fullscreen')
}
</script>

<style scoped>
.guard-page__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.guard-page__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}

.guard-page__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-color-primary);
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.15);
}

.guard-page__sub {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}
</style>
