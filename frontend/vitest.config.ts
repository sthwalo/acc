import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/__tests__/setup/vitest.setup.ts',
    include: ['src/__tests__/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'src/__tests__/',
        '**/*.d.ts',
        '**/*.config.*',
        '**/mockData',
        '**/dist'
      ]
    }
  }
});
