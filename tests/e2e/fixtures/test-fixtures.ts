import { test as base } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { AdminDashboardPage } from '../pages/AdminDashboardPage';
import { PaymentsPage } from '../pages/PaymentsPage';
import { TransactionsPage } from '../pages/TransactionsPage';

type Fixtures = {
  loginPage: LoginPage;
  adminDashboard: AdminDashboardPage;
  paymentsPage: PaymentsPage;
  transactionsPage: TransactionsPage;
};

export const test = base.extend<Fixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  adminDashboard: async ({ page }, use) => {
    await use(new AdminDashboardPage(page));
  },
  paymentsPage: async ({ page }, use) => {
    await use(new PaymentsPage(page));
  },
  transactionsPage: async ({ page }, use) => {
    await use(new TransactionsPage(page));
  },
});

export { expect } from '@playwright/test';
