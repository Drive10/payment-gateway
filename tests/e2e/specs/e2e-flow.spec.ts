import { test, expect } from '../fixtures/test-fixtures';

test.describe('E2E Payment Flow', () => {
  test('login and reach admin page', async ({ loginPage, page }) => {
    await loginPage.goto();
    await loginPage.login('admin@payflow.com', 'Test@1234');
    await page.waitForURL(/\/admin/, { timeout: 10000 });
  });

  test('login persists after page reload', async ({ loginPage, page }) => {
    await loginPage.goto();
    await loginPage.login('admin@payflow.com', 'Test@1234');
    await page.waitForURL(/\/admin/, { timeout: 10000 });
    await page.reload();
    await expect(page).toHaveURL(/\/admin/, { timeout: 10000 });
  });
});
