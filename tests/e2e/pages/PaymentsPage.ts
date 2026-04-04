import { type Page, type Locator, expect } from '@playwright/test';

export class PaymentsPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly searchInput: Locator;
  readonly statusFilter: Locator;
  readonly table: Locator;
  readonly tableRows: Locator;
  readonly noPaymentsMessage: Locator;
  readonly refreshButton: Locator;
  readonly downloadButton: Locator;
  readonly retryButton: Locator;
  readonly paginationInfo: Locator;
  readonly prevPageButton: Locator;
  readonly nextPageButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.locator('h1:has-text("My Payments")');
    this.searchInput = page.getByPlaceholder('Search by order reference...');
    this.statusFilter = page.locator('select, [role="combobox"]').first();
    this.table = page.locator('table');
    this.tableRows = this.table.locator('tbody tr');
    this.noPaymentsMessage = page.locator('text=No payments found');
    this.refreshButton = page.locator('[class*="lucide-refresh"]').first();
    this.downloadButton = page.locator('[class*="lucide-file-text"]').first();
    this.retryButton = page.getByRole('button', { name: /retry/i });
    this.paginationInfo = page.locator('text=Showing');
    this.prevPageButton = page.locator('[class*="lucide-chevron-left"]').first();
    this.nextPageButton = page.locator('[class*="lucide-chevron-right"]').first();
  }

  async goto() {
    await this.page.goto('/user/payments');
  }

  async expectLoaded() {
    await expect(this.heading).toBeVisible();
    await expect(this.table).toBeVisible();
  }

  async searchPayments(query: string) {
    await this.searchInput.fill(query);
  }

  async filterByStatus(status: string) {
    await this.statusFilter.click();
    await this.page.getByRole('option', { name: status }).click();
  }

  async getPaymentCount(): Promise<number> {
    const rows = await this.tableRows.all();
    return rows.length;
  }

  async retryPayment(orderReference: string) {
    const row = this.tableRows.filter({ hasText: orderReference });
    const retryBtn = row.getByRole('button', { name: /retry/i });
    await retryBtn.click();
  }

  async downloadInvoice(orderReference: string) {
    const row = this.tableRows.filter({ hasText: orderReference });
    const downloadBtn = row.locator('[class*="lucide-file-text"]').last();
    await downloadBtn.click();
  }

  async goToNextPage() {
    await this.nextPageButton.click();
  }

  async goToPrevPage() {
    await this.prevPageButton.click();
  }

  async expectPaymentInList(orderReference: string) {
    await expect(this.tableRows.filter({ hasText: orderReference }).first()).toBeVisible();
  }

  async expectPaymentNotInList(orderReference: string) {
    await expect(this.tableRows.filter({ hasText: orderReference })).toHaveCount(0);
  }

  async expectNoPayments() {
    await expect(this.noPaymentsMessage).toBeVisible();
  }

  async expectPagination() {
    await expect(this.paginationInfo).toBeVisible();
  }
}
