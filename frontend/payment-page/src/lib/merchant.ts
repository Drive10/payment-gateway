const API = "/api/v1";

async function request(path: string, options: RequestInit = {}) {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${API}${path}`, { ...options, headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data?.error?.message || data?.message || `Request failed (${res.status})`);
  return data?.data ?? data;
}

function getToken(): string | null {
  try {
    const stored = localStorage.getItem("payflow-merchant-auth");
    if (!stored) return null;
    const parsed = JSON.parse(stored);
    return parsed.token || null;
  } catch {
    return null;
  }
}

export const merchantApi = {
  transactions: {
    list: (limit = 50, offset = 0) =>
      request(`/payments/list?limit=${limit}&offset=${offset}`),
    get: (id: string) => request(`/payments/${id}`),
    capture: (id: string) =>
      request(`/payments/${id}/capture`, { method: "POST", body: "{}" }),
    refund: (paymentId: string, amount?: number, reason?: string) =>
      request("/payments/refund", {
        method: "POST",
        body: JSON.stringify({ paymentId, amount, reason, idempotencyKey: crypto.randomUUID() }),
      }),
    status: (id: string) => request(`/payments/${id}/status`),
  },

  analytics: {
    merchant: (merchantId: string) => request(`/analytics/merchant/${merchantId}`),
  },

  finance: {
    balance: (merchantId: string) => request(`/admin/finance/merchants/${merchantId}/balance`),
    ledger: (merchantId: string) => request(`/admin/finance/merchants/${merchantId}/ledger`),
    settlements: (merchantId: string) => request(`/admin/finance/merchants/${merchantId}/settlements`),
    report: (date: string) => request(`/admin/finance/reports/daily?date=${date}`),
    integrity: () => request("/admin/finance/integrity/check"),
  },

  events: {
    dlq: () => request("/admin/events/dlq"),
    status: () => request("/admin/events/status"),
    replay: (eventId: string) => request(`/admin/events/replay/${eventId}`, { method: "POST" }),
  },

  audit: {
    payment: (paymentId: string) => request(`/audit/payment/${paymentId}`),
  },

  plans: {
    list: () => request("/plans"),
    create: (data: Record<string, unknown>) =>
      request("/plans", { method: "POST", body: JSON.stringify(data) }),
    get: (id: string) => request(`/plans/${id}`),
    update: (id: string, data: Record<string, unknown>) =>
      request(`/plans/${id}`, { method: "PUT", body: JSON.stringify(data) }),
    deactivate: (id: string) =>
      request(`/plans/${id}/deactivate`, { method: "POST" }),
  },

  subscriptions: {
    list: () => request("/subscriptions"),
    create: (data: Record<string, unknown>) =>
      request("/subscriptions", { method: "POST", body: JSON.stringify(data) }),
    get: (id: string) => request(`/subscriptions/${id}`),
    cancel: (id: string) =>
      request(`/subscriptions/${id}/cancel`, { method: "POST" }),
    pause: (id: string) =>
      request(`/subscriptions/${id}/pause`, { method: "POST" }),
    resume: (id: string) =>
      request(`/subscriptions/${id}/resume`, { method: "POST" }),
  },

  invoices: {
    list: () => request("/invoices"),
    create: (data: Record<string, unknown>) =>
      request("/invoices", { method: "POST", body: JSON.stringify(data) }),
    get: (number: string) => request(`/invoices/${number}`),
    send: (number: string) =>
      request(`/invoices/${number}/send`, { method: "POST" }),
    pay: (number: string) =>
      request(`/invoices/${number}/pay`, { method: "POST" }),
    cancel: (number: string) =>
      request(`/invoices/${number}/cancel`, { method: "POST" }),
  },

  vault: {
    list: (email: string) => request(`/vault/methods?customerEmail=${encodeURIComponent(email)}`),
    save: (data: Record<string, unknown>) =>
      request("/vault/methods", { method: "POST", body: JSON.stringify(data) }),
    get: (token: string) => request(`/vault/methods/${token}`),
    delete: (token: string) =>
      request(`/vault/methods/${token}`, { method: "DELETE" }),
    setDefault: (token: string) =>
      request(`/vault/methods/${token}/default`, { method: "POST" }),
  },
};
