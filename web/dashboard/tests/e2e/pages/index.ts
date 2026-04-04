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
    this.emailInput = page.locator('input[name="email"], input#email, input[type="email"]').first();
    this.passwordInput = page.locator('input[name="password"], input#password, input[type="password"]').first();
    this.loginButton = page.locator('button[type="submit"], button:has-text("Sign In"), button:has-text("Log In"), button:has-text("Login")').first();
    this.errorMessage = page.locator('text=/invalid|error|incorrect|failed/i').first();
    this.registerLink = page.locator('a:has-text("Register"), a:has-text("Sign Up")').first();
  }

  async goto() {
    await this.page.goto('/login');
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForTimeout(1000);
  }

  async login(email: string, password: string) {
    await this.emailInput.waitFor({ state: 'visible', timeout: 10000 });
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async expectError() {
    await this.errorMessage.waitFor({ state: 'visible', timeout: 10000 });
  }

  async expectSuccess() {
    await this.page.waitForURL(/\/dashboard|\/admin|\/user/, { timeout: 15000 });
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
    this.navMenu = page.locator('nav, [role="navigation"]').first();
    this.transactionsLink = page.locator('a:has-text("Transactions"), a[href*="transactions"]').first();
    this.usersLink = page.locator('a:has-text("Users"), a[href*="users"]').first();
    this.analyticsLink = page.locator('a:has-text("Analytics"), a[href*="analytics"]').first();
    this.logoutButton = page.locator('button:has-text("Logout"), button:has-text("Sign Out"), a:has-text("Logout")').first();
    this.userMenu = page.locator('[data-testid="user-menu"], button:has-text("Admin"), button:has-text("User"), .user-menu').first();
  }

  async goto() {
    await this.page.goto('/admin');
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForTimeout(1000);
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
    this.transactionsTable = page.locator('table').first();
    this.transactionRows = page.locator('tbody tr, [role="row"]').first();
    this.searchInput = page.locator('input[placeholder*="search" i], input[type="search"]').first();
    this.statusFilter = page.locator('select').first();
    this.pagination = page.locator('[role="navigation"]:has-text("pagination"), .pagination').first();
  }

  async goto() {
    await this.page.goto('/admin/transactions');
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForTimeout(1000);
  }

  async expectTableVisible() {
    await expect(this.transactionsTable).toBeVisible({ timeout: 10000 });
  }
}

export class UserDashboardPage {
  readonly page: Page;
  readonly createPaymentLink: Locator;
  readonly paymentsList: Locator;
  readonly ordersList: Locator;

  constructor(page: Page) {
    this.page = page;
    this.createPaymentLink = page.locator('a:has-text("Create Payment"), a[href*="create-payment"]').first();
    this.paymentsList = page.locator('[data-testid="payments-list"], .payments-list, ul:has-text("Payment")').first();
    this.ordersList = page.locator('[data-testid="orders-list"], .orders-list, ul:has-text("Order")').first();
  }

  async goto() {
    await this.page.goto('/user/dashboard');
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForTimeout(1000);
  }

  async navigateToPayments() {
    await this.page.goto('/user/payments');
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForTimeout(1000);
  }

  async navigateToOrders() {
    await this.page.goto('/user/orders');
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForTimeout(1000);
  }
}
