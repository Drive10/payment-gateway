import { Page, Locator, expect } from '@playwright/test';

export class CheckoutPage {
  readonly page: Page;
  readonly amountInput: Locator;
  readonly currencySelect: Locator;
  readonly emailInput: Locator;
  readonly payButton: Locator;
  readonly errorMessage: Locator;
  readonly paymentMethods: Locator;

  constructor(page: Page) {
    this.page = page;
    this.amountInput = page.getByRole('textbox', { name: /amount/i }).or(page.locator('input[type="number"]'));
    this.currencySelect = page.getByRole('combobox', { name: /currency/i }).or(page.locator('select'));
    this.emailInput = page.getByRole('textbox', { name: /email/i });
    this.payButton = page.getByRole('button', { name: /pay|checkout|submit/i });
    this.errorMessage = page.getByText(/error|invalid|failed/i);
    this.paymentMethods = page.getByRole('radiogroup', { name: /payment method/i }).or(page.locator('[data-testid="payment-methods"]'));
  }

  async goto() {
    await this.page.goto('/');
  }

  async fillCheckout(amount: string, currency: string, email: string) {
    if (await this.amountInput.isVisible()) {
      await this.amountInput.fill(amount);
    }
    if (await this.currencySelect.isVisible()) {
      await this.currencySelect.selectOption(currency);
    }
    if (await this.emailInput.isVisible()) {
      await this.emailInput.fill(email);
    }
  }

  async submitPayment() {
    await this.payButton.click();
  }

  async expectError() {
    await expect(this.errorMessage).toBeVisible();
  }

  async expectProcessing() {
    await this.page.waitForURL(/\/processing/, { timeout: 10000 });
  }
}

export class ProcessingPage {
  readonly page: Page;
  readonly processingIndicator: Locator;

  constructor(page: Page) {
    this.page = page;
    this.processingIndicator = page.getByText(/processing|please wait/i).or(page.locator('[data-testid="processing"]'));
  }

  async expectVisible() {
    await expect(this.processingIndicator).toBeVisible();
  }
}

export class SuccessPage {
  readonly page: Page;
  readonly successMessage: Locator;
  readonly transactionId: Locator;
  readonly receiptButton: Locator;
  readonly doneButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.successMessage = page.getByText(/success|payment successful|completed/i);
    this.transactionId = page.getByText(/transaction|reference|id/i);
    this.receiptButton = page.getByRole('button', { name: /receipt|download/i });
    this.doneButton = page.getByRole('button', { name: /done|back to home/i });
  }

  async expectVisible() {
    await expect(this.successMessage).toBeVisible();
    await expect(this.transactionId).toBeVisible();
  }

  async getTransactionId(): Promise<string> {
    const text = await this.transactionId.textContent();
    return text || '';
  }

  async downloadReceipt() {
    if (await this.receiptButton.isVisible()) {
      await this.receiptButton.click();
    }
  }

  async goHome() {
    if (await this.doneButton.isVisible()) {
      await this.doneButton.click();
    } else {
      await this.page.goto('/');
    }
  }
}

export class FailurePage {
  readonly page: Page;
  readonly failureMessage: Locator;
  readonly errorCode: Locator;
  readonly retryButton: Locator;
  readonly homeButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.failureMessage = page.getByText(/failed|payment failed|error/i);
    this.errorCode = page.getByText(/error code|reason/i);
    this.retryButton = page.getByRole('button', { name: /retry|try again/i });
    this.homeButton = page.getByRole('button', { name: /home|back to home/i });
  }

  async expectVisible() {
    await expect(this.failureMessage).toBeVisible();
  }

  async retry() {
    if (await this.retryButton.isVisible()) {
      await this.retryButton.click();
    }
  }

  async goHome() {
    if (await this.homeButton.isVisible()) {
      await this.homeButton.click();
    } else {
      await this.page.goto('/');
    }
  }
}
