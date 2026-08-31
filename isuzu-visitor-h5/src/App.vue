<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import SplashScreen from '@/components/SplashScreen.vue'
import NoticeGate from '@/components/NoticeGate.vue'
import WeChatGate from '@/components/WeChatGate.vue'
import { isWeChat } from '@/utils/wechat'

const route = useRoute()
// 审批页（被访问人经邮件链接进入）不展示开屏动画与进场须知，也豁免微信环境拦截
const isApprove = computed(() => route.name === 'approve')

// 非微信环境阻断：仅生产构建启用（dev 浏览器调试不受影响）；
// 命中时 WeChatGate 全屏常驻且不可关闭，并跳过 Splash/NoticeGate（避免动画空转）
const wechatBlocked = computed(
  () => import.meta.env.PROD && route.name !== 'approve' && !isWeChat()
)

// 开屏动画：每次进入都展示，5 秒自动结束（可跳过）；
// 覆盖层期间路由照常渲染，Entry 的分流请求在底下并发完成
const splashDone = ref(false)
// 离场动画开始（Splash 将淡出的瞬间）：进场须知需提前就位，防止底层页面随 Splash 透明而透出
const splashLeaving = ref(false)
// 进场须知：勾选同意并点击「进入预约」才放行。
// 用 v-show 常驻 DOM（而非 v-if），并在 Splash 离场淡出期间提前显示（z-index 暂时置于其下），
// 使淡出露出的正是进场须知而非底层路由页
const noticeConfirmed = ref(false)
// 进场须知对用户可见（Splash 开始离场、须知层开始露出）：
// NoticeGate 的阅读倒计时自此才启动，而非组件挂载时（挂载早于开屏动画）
const noticeVisible = computed(() => !isApprove.value && splashLeaving.value)
</script>

<template>
  <!-- 非微信环境阻断层：最高层级，无关闭入口，遮挡所有底层内容 -->
  <WeChatGate v-if="wechatBlocked" />
  <SplashScreen
    v-if="!isApprove && !splashDone && !wechatBlocked"
    @leaving="splashLeaving = true"
    @finish="splashDone = true"
  />
  <NoticeGate
    v-show="!isApprove && splashLeaving && (!noticeConfirmed || !splashDone)"
    :class="{ 'notice--under-splash': !isApprove && splashLeaving && !splashDone }"
    :visible="noticeVisible"
    @confirm="noticeConfirmed = true"
  />
  <router-view />
</template>

<style scoped></style>
