<script setup>
defineOptions({ name: 'WeChatGate' })

// 微信环境阻断层：非微信打开时全屏常驻展示，无关闭入口，
// 底层页面被完全遮挡不可操作；z-index 高于 NoticeGate(10000) 与 Splash(9999)
</script>

<template>
  <div class="wechat-gate">
    <div class="wechat-gate-body">
      <van-icon name="wechat" class="wechat-gate-icon" />
      <h1 class="wechat-gate-title">请在微信中扫码打开</h1>
      <p class="wechat-gate-tip">本页面仅支持微信扫码访问，请使用微信扫描二维码重新进入</p>
    </div>
  </div>
</template>

<style scoped>
.wechat-gate {
  position: fixed;
  inset: 0;
  z-index: 10001;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--page-bg);
}

.wechat-gate-body {
  padding: 0 32px;
  text-align: center;
}

.wechat-gate-icon {
  font-size: 64px;
  color: #07c160;
}

.wechat-gate-title {
  margin-top: 24px;
  color: var(--color-title);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.wechat-gate-tip {
  margin-top: 12px;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.6;
}
</style>
