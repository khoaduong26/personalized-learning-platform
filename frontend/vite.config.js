
import { defineConfig, loadEnv } from 'vite'
import { cwd } from 'node:process'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const { VITE_API_TARGET } = loadEnv(mode, cwd(), 'VITE_')
  const apiTarget = VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': apiTarget,
        '/oauth2': apiTarget,
        '/login/oauth2': apiTarget,
        '/ws': { target: apiTarget, changeOrigin: true, ws: true },
      },
    },
  }
})
