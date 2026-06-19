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
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // SSE must not be buffered — flush each chunk immediately
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            // Disable compression/buffering so SSE frames arrive in real time
            proxyRes.headers['cache-control'] = 'no-cache'
            delete proxyRes.headers['content-encoding']
          })
        }
      }
    }
  }
})
