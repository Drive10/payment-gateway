export const PAYMENT_AMOUNT = 500;
export const PAYMENT_NOTE =
  "Secure checkout for a production-style fintech payment flow.";
export const UPI_ID = "nova-demo@upi";
export const STORAGE_KEY = "nova-checkout-transaction";
export const TRANSACTION_MODES = {
  PRODUCTION: "PRODUCTION",
  TEST: "TEST",
};

const API_BASE_URL = import.meta.env.VITE_API_URL ?? "/api";
// Optional merchantId override for environments where the order API doesn't return it
const DEFAULT_MERCHANT_ID = import.meta.env.VITE_MERCHANT_ID ?? null;
const DEFAULT_ERROR_MESSAGE =
  "Unable to reach the payment backend. Confirm the platform is running and try again.";
const CURRENCY_FORMATTER = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

function randomToken(length = 12) {
  return Math.random().toString(36).slice(2, 2 + length);
}

function getSessionValue(key, fallbackFactory) {
  try {
    const existing = sessionStorage.getItem(key);
    if (existing) {
      return existing;
    }
    const created = fallbackFactory();
    sessionStorage.setItem(key, created);
    return created;
  } catch {
    return fallbackFactory();
  }
}

function getSessionKey() {
  return getSessionValue("nova-checkout-session-key", () => randomToken(10));
}

function getRequestSeed() {
  return getSessionValue("nova-checkout-request-seed", () => randomToken(8));
}

function createCorrelationId(prefix) {
  return `${prefix}-${getRequestSeed()}-${Date.now().toString(36)}`;
}

function buildCustomerIdentity() {
  const sessionKey = getSessionKey();
  const firstName = "Nova";
  const lastName = "Demo";
  return {
    email: `nova.${sessionKey}@example.com`,
    firstName,
    lastName,
    password: `Nova${sessionKey}1234`,
  };
}

function persistCheckoutState(value) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value));
}

export function formatCurrency(amount) {
  return CURRENCY_FORMATTER.format(amount);
}

async function apiRequest(path, options = {}) {
  let response;
  const { headers: customHeaders = {}, ...restOptions } = options;
  const requestId =
    customHeaders["X-Request-Id"] ?? createCorrelationId("checkout");

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        "Content-Type": "application/json",
        "X-Request-Id": requestId,
        ...customHeaders,
      },
      ...restOptions,
    });
  } catch {
    throw new Error(DEFAULT_ERROR_MESSAGE);
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok || payload?.success === false) {
    const code = payload?.error?.code;
    const message =
      payload?.error?.message ||
      payload?.message ||
      (typeof payload?.error === "string" ? payload.error : null) ||
      DEFAULT_ERROR_MESSAGE;
    const finalMessage = code ? `[${code}] ${message}` : message;
    throw new Error(finalMessage);
  }

  return payload?.data;
}

async function ensureAccessToken() {
  const customer = buildCustomerIdentity();

  try {
    const registerResult = await apiRequest("/auth/register", {
      method: "POST",
      body: JSON.stringify(customer),
    });
    console.debug("Registered new customer:", customer.email);
  } catch (error) {
    const errorStr = String(error.message).toLowerCase();
    if (!errorStr.includes("exists")) {
      console.error("Registration failed:", error.message);
      throw error;
    }
    console.debug("User already exists, attempting login");
  }

  try {
    const auth = await apiRequest("/auth/login", {
      method: "POST",
      body: JSON.stringify({
        email: customer.email,
        password: customer.password,
      }),
    });

    if (!auth || !auth.accessToken) {
      throw new Error("Authentication failed - no access token received");
    }

    return {
      token: auth.accessToken,
      customer: {
        ...customer,
        id: auth.user?.id,
      },
    };
  } catch (loginError) {
    const errorStr = String(loginError.message).toLowerCase();
    if (errorStr.includes("invalid") || errorStr.includes("credentials")) {
      console.warn("Login failed,可能的密码不匹配，尝试重新注册");
      sessionStorage.removeItem("nova-checkout-session-key");
      sessionStorage.removeItem("nova-checkout-request-seed");
      
      const newCustomer = buildCustomerIdentity();
      await apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify(newCustomer),
      });
      
      const newAuth = await apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: newCustomer.email,
          password: newCustomer.password,
        }),
      });
      
      return {
        token: newAuth.accessToken,
        customer: {
          ...newCustomer,
          id: newAuth.user?.id,
        },
      };
    }
    throw loginError;
  }
}

export async function startCheckout({
  amount,
  method,
  cardholder,
  transactionMode,
}) {
  const { token, customer } = await ensureAccessToken();
  const externalReference = `checkout-${Date.now()}`;
  const provider =
    transactionMode === TRANSACTION_MODES.TEST
      ? "RAZORPAY_SIMULATOR"
      : "RAZORPAY_PRIMARY";
  const correlationId = createCorrelationId("payment");

  const order = await apiRequest("/orders", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "X-Request-Id": correlationId,
    },
    body: JSON.stringify({
      externalReference,
      amount,
      currency: "INR",
      description: "Nova commerce checkout purchase",
      userId: customer?.id,
    }),
  });

  // Debug: log the payment payload that will be sent to the gateway
  console.debug("Payments payload prepare", {
    orderId: order.id,
    method: method === "upi" ? "UPI" : "CARD",
    provider,
    transactionMode,
    notes: null,
  });

  const merchantIdToSend = order?.merchantId ?? DEFAULT_MERCHANT_ID;
  const payment = await apiRequest("/payments", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Idempotency-Key": `pay-${customer.email}-${externalReference}`,
      "X-Request-Id": correlationId,
    },
    body: JSON.stringify({
      orderId: order.id,
      merchantId: merchantIdToSend,
      method: method === "upi" ? "UPI" : "CARD",
      provider,
      transactionMode,
        notes:
          method === "upi"
            ? "Customer selected UPI"
            : `Cardholder: ${ (cardholder.trim() || [customer?.firstName, customer?.lastName].filter(Boolean).join(" ")).trim() }`,
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
    correlationId,
  };

  persistCheckoutState(checkout);
  return checkout;
}

export async function captureCheckout(checkout) {
  const payment = await apiRequest(`/payments/${checkout.payment.id}/capture`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${checkout.token}`,
      "X-Request-Id": checkout.correlationId ?? createCorrelationId("capture"),
    },
    body: JSON.stringify({}),
  });

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
    customerLabel:
      checkout.method === "upi"
        ? checkout.customer.fullName
        : checkout.cardholder.trim() || checkout.customer.fullName,
    environmentLabel:
      payment.transactionMode === TRANSACTION_MODES.TEST
        ? "Sandbox lane"
        : "Primary processor",
    createdAt: createdAt.toISOString(),
    dateLabel: createdAt.toLocaleString("en-IN", {
      dateStyle: "medium",
      timeStyle: "short",
    }),
    correlationId: checkout.correlationId,
  };

  persistCheckoutState(transaction);
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
