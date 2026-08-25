<script setup>
import { ref } from 'vue'

defineOptions({ name: 'NoticeGate' })

// 进场须知拦截层：首屏动效结束后展示，勾选同意并点击「进入预约」才放行，
// 期间底层路由照常分流，仅被本覆盖层遮挡，放行后直接露出已跳转完成的页面
const emit = defineEmits(['confirm'])

const agreed = ref(false)

const items = [
  { icon: '/notice/reception.png', text: '来访人员请在门岗登记，联系被访人员前来接待并全程陪同' },
  { icon: '/notice/workshop.png', text: '来访人员如需进入车间，为保障安全，需佩戴安全帽、临时出入证' },
  { icon: '/notice/parking.png', text: '车辆按要求停放在指定区域，严禁停放在新能源充电桩处或试验车专属停车区和非停车区域' },
  { icon: '/notice/no-smoking.png', text: '请在指定区域吸烟，严禁流动吸烟' },
  { icon: '/notice/no-camera.png', text: '禁止擅自拍照录像' },
  { icon: '/notice/exit.png', text: '驾车离场，需摇下前后车窗并打开后备箱配合检查' },
  { icon: '/notice/camera.png', text: '您已进入视频监控拍摄区域' },
  { icon: '/notice/warning.png', text: '请自觉遵守公司安全环保管理规定' },
]

function confirm() {
  if (!agreed.value) return
  emit('confirm')
}
</script>

<template>
  <div class="notice">
    <div class="notice-header">
      <h1 class="notice-title">进场须知</h1>
    </div>

    <div class="notice-body">
      <div class="notice-card">
        <div v-for="(item, i) in items" :key="i" class="notice-item">
          <img class="notice-icon" :src="item.icon" alt="" />
          <p class="notice-text">{{ item.text }}</p>
        </div>
      </div>
    </div>

    <div class="notice-footer">
      <van-checkbox v-model="agreed" icon-size="18px" class="notice-check">
        <span class="notice-check-label">我已阅读并同意《进场须知》</span>
      </van-checkbox>
      <van-button type="primary" block round :disabled="!agreed" class="notice-btn" @click="confirm">
        进入预约
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.notice {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  background: var(--page-bg);
}

/* 开屏离场淡出期间：置于 Splash(9999) 之下、底层页面之上，遮挡底层路由页防透出 */
.notice--under-splash {
  z-index: 9998;
}

.notice-header {
  padding-top: calc(env(safe-area-inset-top, 0px) + 20px);
  padding-bottom: 12px;
  text-align: center;
}

.notice-title {
  color: var(--color-title);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 2px;
}

.notice-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
  -webkit-overflow-scrolling: touch;
}

.notice-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 4px 16px;
}

.notice-item {
  display: flex;
  align-items: center;
  padding: 14px 0;
}

.notice-item + .notice-item {
  border-top: 1px solid var(--color-border);
}

.notice-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  margin-right: 12px;
  object-fit: contain;
}

.notice-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text);
}

.notice-footer {
  padding: 12px 16px calc(env(safe-area-inset-bottom, 0px) + 16px);
  background: var(--card-bg);
  border-top: 1px solid var(--color-border);
}

.notice-check {
  margin-bottom: 12px;
}

.notice-check-label {
  font-size: 13px;
  color: var(--color-text);
}
</style>
