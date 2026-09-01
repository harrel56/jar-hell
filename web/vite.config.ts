import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vitest/config'
import solid from '@solidjs/vite-plugin'

const backendUrl = 'http://localhost:8060'

export default defineConfig({
  plugins: [
    solid(),
    tailwindcss(),
  ],
  server: {
    port: 3000,
    proxy: {
      '/api': backendUrl,
      '/technical': backendUrl,
    },
  },
  test: {
    environment: 'jsdom',
    globals: false,
    setupFiles: ['./vitest-setup.ts'],
    isolate: false,
  },
  build: {
    target: 'baseline-widely-available',
    assetsInlineLimit: 0
  },
})
