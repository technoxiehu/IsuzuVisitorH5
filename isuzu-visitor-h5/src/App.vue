<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import SplashScreen from '@/components/SplashScreen.vue'
import NoticeGate from '@/components/NoticeGate.vue'

const route = useRoute()
// 审批页（被访问人经邮件链接进入）不展示开屏动画与进场须知
const isApprove = computed(() => route.name === 'approve')

// 开屏动画：每次进入都展示，5 秒自动结束（可跳过）；
// 覆盖层期间路由照常渲染，Entry 的分流请求在底下并发完成
const splashDone = ref(false)
// 离场动画开始（Splash 将淡出的瞬间）：进场须知需提前就位，防止底层页面随 Splash 透明而透出
const splashLeaving = ref(false)
// 进场须知：勾选同意并点击「进入预约」才放行。
// 用 v-show 常驻 DOM（而非 v-if），并在 Splash 离场淡出期间提前显示（z-index 暂时置于其下），
// 使淡出露出的正是进场须知而非底层路由页
const noticeConfirmed = ref(false)
</script>

<template>
  <SplashScreen
    v-if="!isApprove && !splashDone"
    @leaving="splashLeaving = true"
    @finish="splashDone = true"
  />
  <NoticeGate
    v-show="!isApprove && splashLeaving && (!noticeConfirmed || !splashDone)"
    :class="{ 'notice--under-splash': !isApprove && splashLeaving && !splashDone }"
    @confirm="noticeConfirmed = true"
  />
  <router-view />
</template>

<style scoped></style>
