<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

defineOptions({ name: 'SplashScreen' })

// 首屏开屏动画：每次进入随机展示一张厂区实景图，5 秒后自动揭幕，可点击「跳过」提前结束
const emit = defineEmits(['finish'])

const SHOW_MS = 3000 // 动画展示时长
const EXIT_MS = 600 // 揭幕离场动画时长

// 随机选图：setup 中求值一次，本次进入固定
const scenes = ['/splash/aerial.jpg', '/splash/gate.jpg']
const imgSrc = scenes[Math.floor(Math.random() * scenes.length)]

// 标题逐字浮现（delay 与 CSS 时间轴对齐）
const titleChars = '五十铃发动机访客预约'.split('')
const charDelay = (i) => `${350 + i * 90}ms`

const leaving = ref(false)
let showTimer = null
let exitTimer = null

function leave() {
  if (leaving.value) return
  leaving.value = true
  exitTimer = setTimeout(() => emit('finish'), EXIT_MS)
}

onMounted(() => {
  showTimer = setTimeout(leave, SHOW_MS)
})

onUnmounted(() => {
  clearTimeout(showTimer)
  clearTimeout(exitTimer)
})
</script>

<template>
  <div class="splash" :class="{ 'splash--leaving': leaving }">
    <!-- 单幕：厂区实景图全屏 + 蒙层 + 标题 -->
    <div class="splash-scene">
      <img class="splash-img" :src="imgSrc" alt="五十铃厂区" />
      <div class="splash-shade"></div>
      <h1 class="splash-title">
        <span
          v-for="(ch, i) in titleChars"
          :key="i"
          :style="{ animationDelay: charDelay(i) }"
          >{{ ch }}</span
        >
      </h1>
      <p class="splash-subtitle">访客专属</p>
    </div>

    <button class="splash-skip" :disabled="leaving" @click="leave">跳过</button>

    <!-- 底部进度条：与 5 秒时长同步 -->
    <div class="splash-progress"><i></i></div>
  </div>
</template>

<style scoped>
.splash {
  position: fixed;
  inset: 0;
  z-index: 9999;
  overflow: hidden;
  background: #000;
  transition:
    opacity 0.6s ease,
    transform 0.6s ease;
}

/* 揭幕离场：上滑微放大淡出 */
.splash--leaving {
  opacity: 0;
  transform: translateY(-6%) scale(1.05);
  pointer-events: none;
}

/* ---- 场景 ---- */
.splash-scene {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.splash-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  animation: ken-burns 2.2s ease-out forwards;
}
@keyframes ken-burns {
  from {
    transform: scale(1.08);
  }
  to {
    transform: scale(1);
  }
}
.splash-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.55) 0%,
    rgba(0, 0, 0, 0.2) 45%,
    rgba(0, 0, 0, 0.55) 100%
  );
}

/* 标题（白字逐字上浮，顶部居中） */
.splash-title {
  position: absolute;
  top: calc(env(safe-area-inset-top, 0px) + 18vh);
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}
.splash-title span {
  opacity: 0;
  transform: translateY(14px);
  animation: char-rise 0.55s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}
@keyframes char-rise {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 副标题（底部居中，字距展开浮现） */
.splash-subtitle {
  position: absolute;
  right: 0;
  bottom: calc(env(safe-area-inset-bottom, 0px) + 72px);
  left: 0;
  margin: 0;
  font-size: 13px;
  letter-spacing: 4px;
  text-align: center;
  color: #fff;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.5);
  opacity: 0;
  animation: subtitle-in 0.9s ease 1.5s forwards;
}
@keyframes subtitle-in {
  from {
    opacity: 0;
    letter-spacing: 1px;
  }
  to {
    opacity: 1;
    letter-spacing: 4px;
  }
}

/* ---- 底部进度条 ---- */
.splash-progress {
  position: absolute;
  right: 32px;
  bottom: calc(env(safe-area-inset-bottom, 0px) + 48px);
  left: 32px;
  height: 3px;
  overflow: hidden;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.25);
}
.splash-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--color-primary), #6f9bff);
  transform-origin: left;
  animation: progress-fill 5s linear forwards;
}
@keyframes progress-fill {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(1);
  }
}

/* ---- 跳过按钮 ---- */
.splash-skip {
  position: absolute;
  top: calc(env(safe-area-inset-top, 0px) + 18px);
  right: 18px;
  padding: 6px 16px;
  font-size: 13px;
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 999px;
  backdrop-filter: blur(4px);
  opacity: 0;
  animation: skip-in 0.5s ease 1s forwards;
}
.splash-skip:active {
  background: rgba(255, 255, 255, 0.32);
}
@keyframes skip-in {
  to {
    opacity: 1;
  }
}

/* 尊重系统减弱动态效果设置：瞬时完成入场动画，保留静态画面 */
@media (prefers-reduced-motion: reduce) {
  .splash-img,
  .splash-title span,
  .splash-subtitle,
  .splash-skip,
  .splash-progress i {
    animation-duration: 0.01ms;
  }
}
</style>
