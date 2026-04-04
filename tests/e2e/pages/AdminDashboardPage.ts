import { type Page, type Locator } from '@playwright/test';

export class AdminDashboardPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async goto() {
    await this.page.goto('/admin');
  }

  async expectLoaded() {
    await this.page.waitForURL(/\/admin/);
  }

  async getRevenueValue(): Promise<string> {
    return this.page.getByText('Total Revenue').locator('..').locator('..').getByText('$').textContent() || '';
  }

  async getTotalTransactionsValue(): Promise<string> {
    return this.page.getByText('Total Transactions').locator('..').locator('..').locator('div.text-2xl').textContent() || '';
  }
}
