<template>
  <div class="guard-kiosk" :class="{ 'guard-kiosk--mouse-idle': mouseIdle }">
    <!-- 大屏顶栏：标题 + 时钟 + 操作 -->
    <header class="guard-kiosk__header">
      <div class="guard-kiosk__brand">
        <div class="guard-kiosk__title">来访核验 · 大屏</div>
        <div class="guard-kiosk__sub">今日有效审批单据 · 每 10 秒自动刷新</div>
      </div>

      <div class="guard-kiosk__clock">
        <span class="guard-kiosk__time">{{ now.hh }}:{{ now.mm }}:{{ now.ss }}</span>
        <span class="guard-kiosk__date">{{ now.date }} {{ now.weekday }}</span>
      </div>

      <div class="guard-kiosk__ops">
        <el-button plain round class="guard-kiosk__btn" icon="Refresh" @click="refresh">立即刷新</el-button>
        <el-button plain round class="guard-kiosk__btn" icon="Close" @click="closeKiosk">关闭</el-button>
      </div>
    </header>

    <!-- 卡片墙主体 -->
    <main class="guard-kiosk__body">
      <GuardCardWall ref="wallRef" fullscreen />
    </main>
  </div>
</template>

<script setup name="GuardFullscreen">
import { useRouter } from 'vue-router'
import GuardCardWall from './components/GuardCardWall.vue'

const router = useRouter()
const wallRef = ref(null)

const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const now = reactive({ hh: '--', mm: '--', ss: '--', date: '----/--/--', weekday: '' })

// 鼠标空闲自动隐藏（大屏更干净）
const mouseIdle = ref(false)

let clockTimer = null
let idleTimer = null
let closeTimer = null

function pad(n) {
  return String(n).padStart(2, '0')
}

function tickClock() {
  const d = new Date()
  now.hh = pad(d.getHours())
  now.mm = pad(d.getMinutes())
  now.ss = pad(d.getSeconds())
  now.date = `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
  now.weekday = WEEKDAYS[d.getDay()]
}

/** 手动触发一次静默刷新（不重置计时） */
function refresh() {
  const wall = wallRef.value
  if (wall) {
    wall.getList(true)
  }
}

/* ---------- 原生全屏（主题模式：无地址栏/标签栏/工具栏） ---------- */

function enterFullscreen() {
  const el = document.documentElement
  const req = el.requestFullscreen || el.webkitRequestFullscreen || el.msRequestFullscreen
  if (!req) return
  const p = req.call(el)
  // 极少数浏览器拦截时静默保持窗口模式，不弹提示
  if (p && typeof p.catch === 'function') {
    p.catch(() => {})
  }
}

function exitFullscreen() {
  const doc = document
  const exit = doc.exitFullscreen || doc.webkitExitFullscreen || doc.msExitFullscreen
  const target = doc.fullscreenElement || doc.webkitFullscreenElement
  if (target && exit) {
    exit.call(doc)
  }
}

/* ---------- 鼠标空闲自动隐藏 ---------- */

function showMouse() {
  mouseIdle.value = false
  if (idleTimer) clearTimeout(idleTimer)
}

function onMouseMove() {
  showMouse()
  idleTimer = setTimeout(() => {
    mouseIdle.value = true
  }, 3000)
}

/* ---------- 关闭大屏：退出全屏并返回后台核验页 ---------- */

function closeKiosk() {
  exitFullscreen()
  // 待全屏退出动画完成后返回后台核验页
  if (closeTimer) clearTimeout(closeTimer)
  closeTimer = setTimeout(() => {
    router.push('/visitor/guard')
  }, 250)
}

onMounted(() => {
  tickClock()
  clockTimer = setInterval(tickClock, 1000)
  window.addEventListener('mousemove', onMouseMove)
  onMouseMove()
  // 全屏已由后台页在用户手势内请求；此处兜底重试（如刷新/直访本页），被拦截则静默保持窗口模式
  enterFullscreen()
})

onUnmounted(() => {
  clearInterval(clockTimer)
  if (idleTimer) clearTimeout(idleTimer)
  if (closeTimer) clearTimeout(closeTimer)
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

<style scoped>
.guard-kiosk {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  z-index: 9999;
}

/* 鼠标空闲时隐藏指针（大屏更干净） */
.guard-kiosk--mouse-idle {
  cursor: none;
}

/* ---------- 顶栏 ---------- */
.guard-kiosk__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 24px;
  background: linear-gradient(90deg, #1f2d4d 0%, #2d5fc1 60%, #3a7afe 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 21, 41, 0.2);
}

.guard-kiosk__brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.guard-kiosk__title {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 2px;
}

.guard-kiosk__sub {
  font-size: 12px;
  opacity: 0.75;
}

.guard-kiosk__clock {
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.1;
}

.guard-kiosk__time {
  font-size: 40px;
  font-weight: 700;
  font-family: 'JetBrains Mono', Consolas, monospace;
  letter-spacing: 2px;
}

.guard-kiosk__date {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 2px;
}

.guard-kiosk__ops {
  display: flex;
  gap: 10px;
}

.guard-kiosk__btn {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.08);
}

.guard-kiosk__btn:hover {
  color: #fff;
  border-color: #fff;
  background: rgba(255, 255, 255, 0.18);
}

/* ---------- 主体 ---------- */
.guard-kiosk__body {
  flex: 1;
  overflow: auto;
  padding: 20px 24px;
}
</style>
