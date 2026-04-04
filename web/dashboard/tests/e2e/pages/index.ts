import { Page, Locator, expect } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly errorMessage: Locator;
  readonly registerLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.getByRole('textbox', { name: /email/i });
    this.passwordInput = page.getByRole('textbox', { name: /password/i });
    this.loginButton = page.getByRole('button', { name: /sign in|log in|login/i });
    this.errorMessage = page.getByText(/invalid|error|incorrect/i);
    this.registerLink = page.getByRole('link', { name: /register|sign up/i });
  }

  async goto() {
    await this.page.goto('/login');
  }

  async login(email: string, password: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async expectError() {
    await expect(this.errorMessage).toBeVisible();
  }

  async expectSuccess() {
    await this.page.waitForURL(/\/dashboard|\/admin/, { timeout: 10000 });
  }
}

export class DashboardPage {
  readonly page: Page;
  readonly navMenu: Locator;
  readonly transactionsLink: Locator;
  readonly usersLink: Locator;
  readonly analyticsLink: Locator;
  readonly logoutButton: Locator;
  readonly userMenu: Locator;

  constructor(page: Page) {
    this.page = page;
    this.navMenu = page.getByRole('navigation');
    this.transactionsLink = page.getByRole('link', { name: /transactions/i });
    this.usersLink = page.getByRole('link', { name: /users/i });
    this.analyticsLink = page.getByRole('link', { name: /analytics/i });
    this.logoutButton = page.getByRole('button', { name: /logout|sign out/i });
    this.userMenu = page.getByRole('button', { name: /admin|user/i });
  }

  async goto() {
    await this.page.goto('/admin');
  }

  async navigateToTransactions() {
    await this.transactionsLink.click();
    await this.page.waitForURL(/\/admin\/transactions/, { timeout: 10000 });
  }

  async navigateToUsers() {
    await this.usersLink.click();
    await this.page.waitForURL(/\/admin\/users/, { timeout: 10000 });
  }

  async navigateToAnalytics() {
    await this.analyticsLink.click();
    await this.page.waitForURL(/\/admin\/analytics/, { timeout: 10000 });
  }

  async logout() {
    await this.logoutButton.click();
    await this.page.waitForURL(/\/login/, { timeout: 10000 });
  }
}

export class TransactionsPage {
  readonly page: Page;
  readonly transactionsTable: Locator;
  readonly transactionRows: Locator;
  readonly searchInput: Locator;
  readonly statusFilter: Locator;
  readonly pagination: Locator;

  constructor(page: Page) {
    this.page = page;
    this.transactionsTable = page.getByRole('table');
    this.transactionRows = page.getByRole('row').filter({ hasText: /.+/ });
    this.searchInput = page.getByRole('textbox', { name: /search/i });
    this.statusFilter = page.getByRole('combobox', { name: /status/i });
    this.pagination = page.getByRole('navigation', { name: /pagination/i });
  }

  async goto() {
    await this.page.goto('/admin/transactions');
  }

  async searchTransaction(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForTimeout(500);
  }

  async filterByStatus(status: string) {
    await this.statusFilter.selectOption(status);
    await this.page.waitForTimeout(500);
  }

  async getTransactionCount(): Promise<number> {
    const rows = await this.transactionRows.all();
    return rows.length - 1; // Subtract header row
  }

  async expectTableVisible() {
    await expect(this.transactionsTable).toBeVisible();
  }
}

export class UserDashboardPage {
  readonly page: Page;
  readonly createPaymentLink: Locator;
  readonly paymentsList: Locator;
  readonly ordersList: Locator;

  constructor(page: Page) {
    this.page = page;
    this.createPaymentLink = page.getByRole('link', { name: /create payment/i });
    this.paymentsList = page.getByRole('list', { name: /payments/i }).or(page.locator('[data-testid="payments-list"]'));
    this.ordersList = page.getByRole('list', { name: /orders/i }).or(page.locator('[data-testid="orders-list"]'));
  }

  async goto() {
    await this.page.goto('/user/dashboard');
  }

  async navigateToPayments() {
    await this.page.goto('/user/payments');
  }

  async navigateToOrders() {
    await this.page.goto('/user/orders');
  }
}
