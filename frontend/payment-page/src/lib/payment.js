export const DEFAULT_PAYMENT_NOTE = "Payment for order";
export const UPI_ID = "payflow@upi";
export const STORAGE_KEY = "payflow-checkout-transaction";
export const STORAGE_KEY_AUTH = "payflow-auth";
export const TRANSACTION_MODES = {
  PRODUCTION: "PRODUCTION",
  TEST: "TEST",
};

const API_BASE_URL = window.__ENV__?.API_BASE_URL 
  || import.meta.env.VITE_API_GATEWAY_URL 
  || "http://localhost:8083";
const DEFAULT_MERCHANT_ID = window.__ENV__?.MERCHANT_ID || import.meta.env.VITE_MERCHANT_ID || "11c16124-5407-46cf-812c-8b36f7c894b7";
const MERCHANT_API_KEY = window.__ENV__?.MERCHANT_API_KEY || import.meta.env.VITE_MERCHANT_API_KEY || "sk_test_88573d07c94d45f58ead0e698918f420";
const DEFAULT_ERROR_MESSAGE =
  "Unable to reach the payment backend. Confirm the platform is running and try again.";
const CURRENCY_FORMATTER = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

function createApiError(message, status, code) {
  const error = new Error(message);
  error.status = status;
  error.code = code;
  return error;
}

function randomToken(length = 12) {
  return Math.random().toString(36).slice(2, 2 + length);
}

function createCorrelationId(prefix) {
  return `${prefix}-${randomToken(8)}-${Date.now().toString(36)}`;
}

function buildCustomerIdentity(email, firstName, lastName) {
  const sessionKey = randomToken(10);
  return {
    email: email || `user.${sessionKey}@example.com`,
    firstName: firstName || "Customer",
    lastName: lastName || "User",
    password: `Payflow${sessionKey}123`,
  };
}

function persistCheckoutState(value) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value));
}

function persistAuth(value) {
  localStorage.setItem(STORAGE_KEY_AUTH, JSON.stringify(value));
}

export function getStoredAuth() {
  try {
    const value = localStorage.getItem(STORAGE_KEY_AUTH);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

export function clearAuth() {
  localStorage.removeItem(STORAGE_KEY_AUTH);
  sessionStorage.removeItem(STORAGE_KEY);
}

export function formatCurrency(amount) {
  return CURRENCY_FORMATTER.format(amount);
}

async function apiRequest(path, options = {}) {
  let response;
  const { headers: customHeaders = {}, timeout = 30000, retries = 2, ...restOptions } = options;
  const requestId = customHeaders["X-Request-Id"] ?? createCorrelationId("checkout");
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      response = await fetch(`${API_BASE_URL}${path}`, {
        headers: { "Content-Type": "application/json", "X-Request-Id": requestId, ...customHeaders },
        ...restOptions,
        signal: controller.signal,
      });
      break;
    } catch (error) {
      if (attempt < retries && (error.name === "TypeError" || error.message.includes("network"))) {
        await new Promise(r => setTimeout(r, Math.pow(2, attempt) * 1000));
        continue;
      }
      clearTimeout(timeoutId);
      if (error.name === "AbortError") {
        throw createApiError("Request timed out", 408, "REQUEST_TIMEOUT");
      }
      throw createApiError(DEFAULT_ERROR_MESSAGE, 0, "NETWORK_ERROR");
    }
  }
  clearTimeout(timeoutId);

  const payload = await response.json().catch(() => null);

  if (!response.ok || payload?.success === false) {
    const code = payload?.error?.code;
    const message =
      payload?.error?.message ||
      payload?.message ||
      (typeof payload?.error === "string" ? payload.error : null) ||
      DEFAULT_ERROR_MESSAGE;
    const finalMessage = code ? `[${code}] ${message}` : message;
    throw createApiError(finalMessage, response.status, code || "API_ERROR");
  }

  return payload?.data;
}

export async function login(email, password) {
  const auth = await apiRequest("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

  if (!auth || !auth.accessToken) {
    throw new Error("Login failed - no access token received");
  }

  const authData = {
    token: auth.accessToken,
    refreshToken: auth.refreshToken,
    user: auth.user,
    expiresAt: Date.now() + (auth.expiresIn || 3600) * 1000,
  };
  persistAuth(authData);
  return authData;
}

export async function register(email, password, firstName, lastName) {
  const customer = buildCustomerIdentity(email, firstName, lastName);
  if (email) customer.email = email;
  if (firstName) customer.firstName = firstName;
  if (lastName) customer.lastName = lastName;
  if (password) customer.password = password;

  await apiRequest("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(customer),
  });

  return login(customer.email, customer.password);
}

export async function ensureAccessToken() {
  return {
    token: MERCHANT_API_KEY,
    customer: { email: "customer@example.com", firstName: "Guest", lastName: "User" }
  };
}

export async function logout() {
  clearAuth();
}

export async function startCheckout({
  amount,
  method,
  cardholder,
  transactionMode,
  description,
  customerEmail,
  customerName,
}) {
  const externalReference = `pay-${Date.now()}-${randomToken(6)}`;
  const provider =
    transactionMode === TRANSACTION_MODES.TEST
      ? "RAZORPAY_SIMULATOR"
      : "RAZORPAY_PRIMARY";
  const correlationId = createCorrelationId("payment");

  const executeCheckout = async () => {
    const order = await apiRequest("/api/v1/payments/create-order", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${MERCHANT_API_KEY}`,
        "X-Request-Id": correlationId,
      },
      body: JSON.stringify({
        orderId: externalReference,
        amount: amount,
        currency: "INR",
        description: description || `Payment for order ${externalReference}`,
customerEmail: customerEmail || "customer@example.com",
        customerName: customerName || "Customer",
      }),
    });

    const merchantIdToSend = order?.merchantId ?? DEFAULT_MERCHANT_ID;
    const idempotencyKey = `pay-guest-${externalReference}`;
    const payment = await apiRequest("/api/v1/payments", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${MERCHANT_API_KEY}`,
        "Idempotency-Key": idempotencyKey,
        "X-Request-Id": correlationId,
      },
      body: JSON.stringify({
        orderId: order.paymentId,
        merchantId: merchantIdToSend,
        method: method === "upi" ? "UPI" : method === "netbanking" ? "NET_BANKING" : method === "wallet" ? "WALLET" : "CARD",
        provider,
        transactionMode,
        notes:
          method === "upi"
            ? "Customer selected UPI"
            : method === "netbanking"
              ? "Customer selected Net Banking"
              : method === "wallet"
                ? "Customer selected Wallet"
                : `Cardholder: ${cardholder.trim() || "Customer"}`,
      }),
    });

    const checkout = {
      token: MERCHANT_API_KEY,
      order,
      payment,
      amount,
      method,
      cardholder,
      correlationId,
    };

    persistCheckoutState(checkout);
    return checkout;
  };

  try {
    return await executeCheckout();
  } catch (error) {
    throw error;
  }
}

export async function captureCheckout(checkout, onStatusChange) {
  const maxAttempts = 10;
  const pollInterval = 3000;
  
  for (let attempt = 0; attempt <= maxAttempts; attempt++) {
    try {
      const payment = await apiRequest(`/api/v1/payments/${checkout.payment.paymentId}/capture`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${MERCHANT_API_KEY}`,
          "X-Request-Id": checkout.correlationId ?? createCorrelationId("capture"),
        },
        body: JSON.stringify({}),
      });

      if (payment.status !== "PROCESSING") {
        const createdAt = payment.createdAt ? new Date(payment.createdAt) : new Date();
        const methodLabel = checkout.method === "upi" ? "UPI" : "Card";

        const transaction = {
          id: payment.id,
          orderId: payment.orderId,
          orderReference: payment.orderReference,
          providerOrderId: payment.providerOrderId,
          providerPaymentId: payment.providerPaymentId,
          transactionMode: payment.transactionMode,
          simulated: payment.simulated,
          amount: checkout.amount,
          amountLabel: formatCurrency(checkout.amount),
          method: checkout.method,
          methodLabel,
          status: payment.status,
          customerLabel: checkout.method === "upi" ? checkout.customer?.fullName || checkout.customer?.firstName : checkout.cardholder.trim() || `${checkout.customer?.firstName} ${checkout.customer?.lastName}`.trim(),
          environmentLabel: payment.transactionMode === TRANSACTION_MODES.TEST ? "Sandbox lane" : "Primary processor",
          createdAt: createdAt.toISOString(),
          dateLabel: createdAt.toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }),
          correlationId: checkout.correlationId,
          errorMessage: payment.errorMessage,
          errorCode: payment.errorCode,
        };

        persistCheckoutState(transaction);
        return transaction;
      }

      if (onStatusChange) onStatusChange(payment.status, attempt + 1, maxAttempts);
      if (attempt < maxAttempts) await new Promise(r => setTimeout(r, pollInterval));
    } catch (error) {
      if (attempt < maxAttempts) {
        await new Promise(r => setTimeout(r, pollInterval));
        continue;
      }
      throw error;
    }
  }
  
  throw new Error("Payment verification timed out. Please check payment status in your dashboard.");
}

export async function getPaymentStatus(paymentId, token) {
  return apiRequest(`/api/v1/payments/${paymentId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function getOrderHistory(token, limit = 10, offset = 0) {
  return apiRequest(`/api/v1/payments/orders?limit=${limit}&offset=${offset}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function getPaymentHistory(token, limit = 10, offset = 0) {
  return apiRequest(`/api/v1/payments?limit=${limit}&offset=${offset}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function formatCardNumber(value) {
  return value
    .replace(/\D/g, "")
    .slice(0, 16)
    .replace(/(\d{4})(?=\d)/g, "$1 ")
    .trim();
}

export function formatExpiry(value) {
  const digits = value.replace(/\D/g, "").slice(0, 4);

  if (digits.length < 3) {
    return digits;
  }

  return `${digits.slice(0, 2)}/${digits.slice(2)}`;
}

export function detectCardBrand(value) {
  const digits = value.replace(/\D/g, "");

  if (digits.startsWith("4")) {
    return "Visa";
  }

  if (/^5[1-5]/.test(digits)) {
    return "Mastercard";
  }

  if (/^3[47]/.test(digits)) {
    return "Amex";
  }

  if (/^6/.test(digits)) {
    return "RuPay";
  }

  return "Card";
}

export function isValidCardNumber(value) {
  const digits = value.replace(/\D/g, "");

  if (digits.length !== 16) {
    return false;
  }

  let sum = 0;
  let shouldDouble = false;

  for (let index = digits.length - 1; index >= 0; index -= 1) {
    let digit = Number(digits[index]);

    if (shouldDouble) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }

    sum += digit;
    shouldDouble = !shouldDouble;
  }

  return sum % 10 === 0;
}

export function validateCardForm(values) {
  const errors = {};

  if (!isValidCardNumber(values.cardNumber)) {
    errors.cardNumber = "Enter a valid 16-digit card number.";
  }

  if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(values.expiry)) {
    errors.expiry = "Use MM/YY format.";
  }

  if (!/^\d{3}$/.test(values.cvv)) {
    errors.cvv = "CVV must be 3 digits.";
  }

  if (!values.cardholder.trim()) {
    errors.cardholder = "Cardholder name is required.";
  }

  return errors;
}

export function getStoredTransaction() {
  try {
    const value = sessionStorage.getItem(STORAGE_KEY);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

export function clearTransaction() {
  sessionStorage.removeItem(STORAGE_KEY);
}
