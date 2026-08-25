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
// 进场须知：开屏结束后展示，勾选同意并点击「进入预约」才放行
const noticeConfirmed = ref(false)
</script>

<template>
  <SplashScreen v-if="!isApprove && !splashDone" @finish="splashDone = true" />
  <NoticeGate
    v-if="!isApprove && splashDone && !noticeConfirmed"
    @confirm="noticeConfirmed = true"
  />
  <router-view />
</template>

<style scoped></style>
