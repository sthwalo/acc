import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Vitest now configured in vitest.config.ts
  build: {
    rollupOptions: {
      // Disable native rollup binaries for ARM64 compatibility
      experimentalLogSideEffects: false,
      // Force rollup to not use native binaries
      onwarn: (warning, warn) => {
        // Suppress native binary warnings
        if (warning.code === 'PLUGIN_WARNING' && warning.plugin === 'rollup' && warning.message.includes('native')) {
          return;
        }
        warn(warning);
      },
    },
  },
  server: {
    // Container-first development: Proxy to containerized backend
    port: 3000, // Match backend CORS configuration
    proxy: {
      '/api': {
        target: process.env.CONTAINER_MODE === 'true' ? 'http://fin-app:8080' : 'http://localhost:8080', // Use service name in container, localhost on host
        changeOrigin: true,
        secure: false,
        configure: (proxy, options) => {
          proxy.on('error', (err) => {
            console.error('Proxy error:', err);
          });
          proxy.on('proxyReq', (_, req) => {
            const url = req.url ?? '';
            console.log('Proxying request:', req.method, url, '->', options.target + url);
          });
          // Log proxied responses to detect any header/content modifications
          proxy.on('proxyRes', (proxyRes, req) => {
            try {
              console.log('Proxy response:', req.method, req.url, 'status=', proxyRes.statusCode, 'headers=', proxyRes.headers);
            } catch (e) {
              console.warn('Failed to log proxy response:', e);
            }
          });
        }
      }
    },
    // CORS enabled for container communication
    cors: true,
    host: true, // Allow external connections for container access
  },
  // Environment variables for container-first development
  define: {
    __CONTAINER_MODE__: JSON.stringify(process.env.CONTAINER_MODE === 'true'),
  },
});
