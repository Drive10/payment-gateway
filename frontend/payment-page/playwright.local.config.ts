import { defineConfig, devices } from '@playwright/test';

const e2eBaseUrl = process.env.E2E_BASE_URL ?? 'http://localhost:5173';

export default defineConfig({
  testDir: './tests/e2e/specs',
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: [['html', { outputFolder: '../test-results/html' }], ['list']],
  use: {
    baseURL: e2eBaseUrl,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
