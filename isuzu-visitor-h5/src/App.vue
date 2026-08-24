<script setup>
import { ref } from 'vue'
import SplashScreen from '@/components/SplashScreen.vue'

// 开屏动画：每次进入都展示，5 秒自动结束（可跳过）；
// 覆盖层期间路由照常渲染，Entry 的分流请求在底下并发完成
const splashDone = ref(false)
</script>

<template>
  <SplashScreen v-if="!splashDone" @finish="splashDone = true" />
  <router-view />
</template>

<style scoped></style>
