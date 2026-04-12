import { expect, test } from "@playwright/test";

const AUTH_STORAGE_KEY = "payflow-auth";
const TX_STORAGE_KEY = "payflow-checkout-transaction";

test.describe("Fullstack checkout E2E", () => {
  test.setTimeout(120_000);

  test("creates and verifies payment across frontend + backend", async ({ page, request }) => {
    await page.goto("/");

    const amountToggle = page.getByRole("button", { name: /enter custom amount/i });
    await expect(amountToggle).toBeVisible();
    await amountToggle.click();

    const amountInput = page.getByPlaceholder("Enter amount");
    await amountInput.fill("1200");

    const upiButton = page.getByRole("button", { name: /^UPI$/ });
    await upiButton.click();

    const payButton = page.getByRole("button", { name: /^Pay /i });
    await expect(payButton).toBeEnabled();
    await payButton.click();

    await page.waitForURL(/\/(success|failure)/, { timeout: 90_000 });
    expect(page.url(), "checkout should complete successfully").toMatch(/\/success/);

    await expect(page.getByRole("heading", { name: /payment successful/i })).toBeVisible();

    const authState = await page.evaluate((key) => {
      const raw = window.localStorage.getItem(key);
      return raw ? JSON.parse(raw) : null;
    }, AUTH_STORAGE_KEY);
    expect(authState?.token, "frontend should persist auth token").toBeTruthy();

    const txState = await page.evaluate((key) => {
      const raw = window.sessionStorage.getItem(key);
      return raw ? JSON.parse(raw) : null;
    }, TX_STORAGE_KEY);
    expect(txState?.id, "frontend should persist transaction id").toBeTruthy();
    expect(txState?.orderId, "frontend should persist order id").toBeTruthy();

    const authHeaders = { Authorization: `Bearer ${authState.token}` };
    const paymentId = txState.id;
    const orderId = txState.orderId;

    const paymentResponse = await request.get(`/api/v1/payments/${paymentId}`, {
      headers: authHeaders,
    });
    expect(paymentResponse.ok(), "payment lookup should succeed").toBeTruthy();

    const paymentBody = await paymentResponse.json();
    expect(paymentBody?.success).toBeTruthy();
    expect(paymentBody?.data?.id).toBe(paymentId);
    expect(["CREATED", "PROCESSING", "COMPLETED", "SUCCESS", "CAPTURED"]).toContain(
      paymentBody?.data?.status,
    );

    const paymentsResponse = await request.get("/api/v1/payments?limit=20&offset=0", {
      headers: authHeaders,
    });
    expect(paymentsResponse.ok(), "payment history lookup should succeed").toBeTruthy();

    const paymentsBody = await paymentsResponse.json();
    expect(paymentsBody?.success).toBeTruthy();
    const payments = Array.isArray(paymentsBody?.data?.content) ? paymentsBody.data.content : [];
    expect(
      payments.some((payment: { id?: string; orderId?: string }) => payment?.id === paymentId && payment?.orderId === orderId),
      "captured payment should be visible in backend payment history",
    ).toBeTruthy();
  });
});
