import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 8080,
    open: false,
    host: true,
    allowedHosts: true,
    proxy: {
      '/api/railradar': {
        target: 'https://railradar.in/api/v1',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/railradar/, '')
      }
    }
  }
});