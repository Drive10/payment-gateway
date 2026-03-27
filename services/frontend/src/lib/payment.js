export const PAYMENT_AMOUNT = 500;
export const PAYMENT_NOTE = "Integrated demo flow backed by the fintech backend.";
export const UPI_ID = "nova-demo@upi";
export const STORAGE_KEY = "nova-checkout-transaction";
const API_BASE_URL = import.meta.env.VITE_API_URL ?? "/api";
const DEFAULT_ERROR_MESSAGE =
  "Unable to reach the payment backend. Confirm Docker services are up and try again.";

function getSessionKey() {
  try {
    const existing = sessionStorage.getItem("nova-checkout-session-key");
    if (existing) {
      return existing;
    }
    const created = Math.random().toString(36).slice(2, 10);
    sessionStorage.setItem("nova-checkout-session-key", created);
    return created;
  } catch {
    return Math.random().toString(36).slice(2, 10);
  }
}

function buildCustomerIdentity() {
  const sessionKey = getSessionKey();
  return {
    email: `nova.${sessionKey}@example.com`,
    fullName: "Nova Demo Customer",
    password: "User1234",
  };
}

async function apiRequest(path, options = {}) {
  let response;
  const { headers: customHeaders = {}, ...restOptions } = options;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        "Content-Type": "application/json",
        ...customHeaders,
      },
      ...restOptions,
    });
  } catch {
    throw new Error(DEFAULT_ERROR_MESSAGE);
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok || payload?.success === false) {
    const message =
      payload?.error?.message ||
      payload?.message ||
      (typeof payload?.error === "string" ? payload.error : null) ||
      DEFAULT_ERROR_MESSAGE;
    throw new Error(message);
  }

  return payload?.data;
}

async function ensureAccessToken() {
  const customer = buildCustomerIdentity();

  try {
    await apiRequest("/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(customer),
    });
  } catch (error) {
    if (!String(error.message).toLowerCase().includes("already exists")) {
      throw error;
    }
  }

  const auth = await apiRequest("/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({
      email: customer.email,
      password: customer.password,
    }),
  });

  return {
    token: auth.accessToken,
    customer,
  };
}

export async function startCheckout({ amount, method, cardholder }) {
  const { token, customer } = await ensureAccessToken();
  const externalReference = `checkout-${Date.now()}`;

  const order = await apiRequest("/v1/orders", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      externalReference,
      amount,
      currency: "INR",
      description: "Nova premium checkout purchase",
    }),
  });

  const payment = await apiRequest("/v1/payments", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Idempotency-Key": `pay-${externalReference}`,
    },
    body: JSON.stringify({
      orderId: order.id,
      method: method === "upi" ? "UPI" : "CARD",
      provider: "razorpay_simulator",
      notes: method === "upi" ? "Customer selected UPI" : `Cardholder: ${cardholder.trim() || customer.fullName}`,
    }),
  });

  const checkout = {
    token,
    customer,
    order,
    payment,
    amount,
    method,
    cardholder,
  };

  localStorage.setItem(STORAGE_KEY, JSON.stringify(checkout));
  return checkout;
}

export async function captureCheckout(checkout) {
  const payment = await apiRequest(`/v1/payments/${checkout.payment.id}/capture`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${checkout.token}`,
    },
    body: JSON.stringify({
      providerPaymentId: `pay_sim_${checkout.payment.id.slice(0, 8)}`,
      providerSignature: `sig_${checkout.order.orderReference}`,
    }),
  });

  const createdAt = payment.createdAt ? new Date(payment.createdAt) : new Date();
  const methodLabel = checkout.method === "upi" ? "UPI" : "Card";

  const transaction = {
    id: payment.id,
    orderId: payment.orderId,
    orderReference: payment.orderReference,
    providerOrderId: payment.providerOrderId,
    providerPaymentId: payment.providerPaymentId,
    amount: checkout.amount,
    amountLabel: `₹${checkout.amount}`,
    method: checkout.method,
    methodLabel,
    status: payment.status,
    customerLabel: checkout.method === "upi" ? checkout.customer.fullName : checkout.cardholder.trim() || checkout.customer.fullName,
    createdAt: createdAt.toISOString(),
    dateLabel: createdAt.toLocaleString("en-IN", {
      dateStyle: "medium",
      timeStyle: "short",
    }),
  };

  localStorage.setItem(STORAGE_KEY, JSON.stringify(transaction));
  return transaction;
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

export function buildTransaction({ amount, method, cardholder }) {
  const createdAt = new Date();
  const methodLabel = method === "upi" ? "UPI" : "Card";

  return {
    id: `txn_${Math.random().toString(36).slice(2, 10)}`,
    amount,
    amountLabel: `₹${amount}`,
    method,
    methodLabel,
    status: "SUCCESS",
    customerLabel: method === "upi" ? "UPI Customer" : cardholder.trim(),
    createdAt: createdAt.toISOString(),
    dateLabel: createdAt.toLocaleString("en-IN", {
      dateStyle: "medium",
      timeStyle: "short",
    }),
  };
}

export function getStoredTransaction() {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}
