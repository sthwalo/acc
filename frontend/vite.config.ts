import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
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
})
