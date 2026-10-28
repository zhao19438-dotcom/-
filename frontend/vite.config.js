import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

// 校园论坛前端：开发端口 5173，接口通过 proxy 转发到后端 58080（与 application.yml 一致），同源无跨域。
export default defineConfig({
  plugins: [vue(), Components({ dts: false, resolvers: [ElementPlusResolver({ importStyle: 'css' })] })],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/user': 'http://127.0.0.1:58080',
      '/article': 'http://127.0.0.1:58080',
      '/reply': 'http://127.0.0.1:58080',
      '/board': 'http://127.0.0.1:58080',
      '/message': 'http://127.0.0.1:58080',
      '/admin': 'http://127.0.0.1:58080',
      '/uploads': 'http://127.0.0.1:58080'
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
