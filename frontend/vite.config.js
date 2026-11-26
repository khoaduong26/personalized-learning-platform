
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const { VITE_API_TARGET } = loadEnv(mode, process.cwd(), 'VITE_')
  const apiTarget = VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [react(), tailwindcss()],
    server: {
      proxy: {
        '/api': apiTarget,
        '/oauth2': apiTarget,
        '/login/oauth2': apiTarget,
        '/ws': { target: apiTarget, changeOrigin: true, ws: true },
      },
    },
  }
})
