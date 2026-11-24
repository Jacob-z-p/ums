import { defineConfig, devices } from '@playwright/test';

const integration = process.env.UMS_E2E_INTEGRATION === '1';
const baseURL = integration ? 'http://localhost:8000' : 'http://localhost:8001';

export default defineConfig({
  testDir: './e2e',
  testMatch: integration ? '**/*.integration.spec.ts' : '**/*.spec.ts',
  testIgnore: integration ? undefined : '**/*.integration.spec.ts',
  outputDir: integration
    ? './e2e-out/integration-results'
    : './e2e-out/test-results',
  reporter: [
    ['list'],
    [
      'html',
      {
        outputFolder: integration
          ? 'e2e-out/integration-report'
          : 'e2e-out/playwright-report',
        open: 'never',
      },
    ],
  ],
  workers: 1,
  timeout: 90_000,
  expect: { timeout: 30_000 },
  forbidOnly: !!process.env.CI,
  retries: process.env.CI && !integration ? 2 : 0,
  use: {
    baseURL,
    trace: integration ? 'off' : 'on-first-retry',
    locale: 'zh-CN',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: integration
      ? 'npm start'
      : 'npm run build && npm exec -- max preview --port 8001',
    env: integration ? { UMS_E2E: '1' } : undefined,
    url: baseURL,
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
