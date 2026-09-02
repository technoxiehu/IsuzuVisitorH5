import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    open: true,
    proxy: {
      // 访客接口代理到若依后端（docs/03_接口契约.md §6）
      '/visitor': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 头像等上传文件代理到后端（后端 /profile/** -> file:{profile}/）
      '/profile': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
