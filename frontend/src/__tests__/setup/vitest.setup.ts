/**
 * Vitest setup file for FIN frontend tests
 * Configures testing environment for unit tests (node environment)
 */

import { afterEach, vi } from 'vitest';

// Cleanup mocks after each test
afterEach(() => {
  vi.clearAllMocks();
});


