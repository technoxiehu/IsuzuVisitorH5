import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'entry', component: () => import('@/views/Entry.vue') },
    { path: '/user-info', name: 'userInfo', component: () => import('@/views/UserInfo.vue') },
    { path: '/application', name: 'application', component: () => import('@/views/Application.vue') },
    { path: '/list', name: 'list', component: () => import('@/views/List.vue') },
    { path: '/approve', name: 'approve', component: () => import('@/views/Approve.vue') },
  ],
})

export default router
