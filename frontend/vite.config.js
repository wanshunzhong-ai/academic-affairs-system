import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    // 端口固定 5173，与后端 application.yml 的 aas.frontend-url 保持一致
    // —— 后端启动横幅打印的「登录入口」就是从那里派生的。
    //
    // strictPort: true = 端口被占用时直接报错退出，而不是静默顺延到 5174/5175……
    // Vite 默认 strictPort 是 false，会悄悄地换端口，结果就是
    // 「横幅打印 http://localhost:5173/login」而「浏览器实际是 5177」对不上。
    port: 5173,
    strictPort: true,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 2000,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          element: ['element-plus', '@element-plus/icons-vue'],
          echarts: ['echarts']
        }
      }
    }
  }
})
