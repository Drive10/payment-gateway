import { type Page, type Locator, expect } from '@playwright/test';

export class TransactionsPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly searchInput: Locator;
  readonly statusFilter: Locator;
  readonly table: Locator;
  readonly tableRows: Locator;
  readonly noTransactionsMessage: Locator;
  readonly paginationInfo: Locator;
  readonly prevPageButton: Locator;
  readonly nextPageButton: Locator;
  readonly downloadButton: Locator;
  readonly viewDetailButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.locator('h1:has-text("Transactions")');
    this.searchInput = page.getByPlaceholder('Search transactions...');
    this.statusFilter = page.locator('select, [role="combobox"]').first();
    this.table = page.locator('table');
    this.tableRows = this.table.locator('tbody tr');
    this.noTransactionsMessage = page.locator('text=No transactions found');
    this.paginationInfo = page.locator('text=Showing');
    this.prevPageButton = page.locator('[class*="lucide-chevron-left"]').first();
    this.nextPageButton = page.locator('[class*="lucide-chevron-right"]').first();
    this.downloadButton = page.locator('[class*="lucide-download"]').first();
    this.viewDetailButton = page.locator('[class*="lucide-eye"]').first();
  }

  async goto() {
    await this.page.goto('/admin/transactions');
  }

  async expectLoaded() {
    await expect(this.heading).toBeVisible();
    await expect(this.table).toBeVisible();
  }

  async searchTransactions(query: string) {
    await this.searchInput.fill(query);
  }

  async filterByStatus(status: string) {
    await this.statusFilter.click();
    await this.page.getByRole('option', { name: status }).click();
  }

  async getTransactionCount(): Promise<number> {
    const rows = await this.tableRows.all();
    return rows.length;
  }

  async goToNextPage() {
    await this.nextPageButton.click();
  }

  async goToPrevPage() {
    await this.prevPageButton.click();
  }

  async expectTransactionInList(orderReference: string) {
    await expect(this.tableRows.filter({ hasText: orderReference }).first()).toBeVisible();
  }

  async expectTransactionNotInList(orderReference: string) {
    await expect(this.tableRows.filter({ hasText: orderReference })).toHaveCount(0);
  }

  async expectNoTransactions() {
    await expect(this.noTransactionsMessage).toBeVisible();
  }

  async expectPagination() {
    await expect(this.paginationInfo).toBeVisible();
  }

  async clickViewDetail(orderReference: string) {
    const row = this.tableRows.filter({ hasText: orderReference });
    await row.locator('a').first().click();
  }
}
