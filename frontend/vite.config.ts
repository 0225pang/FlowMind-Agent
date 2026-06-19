import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    allowedHosts: ['345cca50.r28.cpolar.top', '1ba8ba83.r28.cpolar.top'],
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
