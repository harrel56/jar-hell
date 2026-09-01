import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vitest/config';
import solid from '@solidjs/vite-plugin';

export default defineConfig({
  // Plain SPA mode: index.html is the entry and src/index.tsx mounts the app.
  // No generated entries, no prerendered shell, no dist/server — vite build
  // emits a static dist/ that Javalin serves as-is.
  plugins: [
    solid(),
    // Scans source files for class names and generates their CSS into the
    // stylesheet that imports tailwindcss (src/App.css).
    tailwindcss(),
  ],
  server: {
    port: 3000,
  },
  test: {
    environment: 'jsdom',
    globals: false,
    setupFiles: ['./vitest-setup.ts'],
    isolate: false,
  },
  build: {
    target: 'baseline-widely-available',
    // Keep images as asset files instead of inlining them into the JS bundle.
    assetsInlineLimit: 0
  },
});
