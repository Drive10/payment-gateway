import { useState } from "react";

interface PaymentLink {
  id: string;
  referenceId: string;
  amount: number;
  currency: string;
  description: string;
  status: string;
  createdAt: string;
  expiresAt: string;
  url: string;
}

const mockLinks: PaymentLink[] = [
  { id: "pl_001", referenceId: "pay_demo_001", amount: 5000, currency: "INR", description: "Website development", status: "active", createdAt: "2026-05-10", expiresAt: "2026-06-10", url: "http://localhost:5173/?ref=pay_demo_001" },
  { id: "pl_002", referenceId: "pay_demo_002", amount: 1200, currency: "USD", description: "Consulting fees", status: "active", createdAt: "2026-05-12", expiresAt: "2026-06-12", url: "http://localhost:5173/?ref=pay_demo_002" },
  { id: "pl_003", referenceId: "pay_demo_003", amount: 2500, currency: "INR", description: "Design services", status: "used", createdAt: "2026-05-05", expiresAt: "2026-06-05", url: "http://localhost:5173/?ref=pay_demo_003" },
  { id: "pl_004", referenceId: "pay_demo_004", amount: 9999, currency: "INR", description: "Annual subscription", status: "expired", createdAt: "2026-03-01", expiresAt: "2026-04-01", url: "http://localhost:5173/?ref=pay_demo_004" },
];

export default function PaymentLinks() {
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ amount: "", description: "", customerEmail: "" });
  const [links, setLinks] = useState(mockLinks);

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    const refId = `pay_${Date.now().toString(36)}`;
    const newLink: PaymentLink = {
      id: `pl_${Date.now()}`,
      referenceId: refId,
      amount: parseFloat(form.amount),
      currency: "INR",
      description: form.description,
      status: "active",
      createdAt: new Date().toISOString().split("T")[0],
      expiresAt: new Date(Date.now() + 30 * 86400000).toISOString().split("T")[0],
      url: `${window.location.origin}/?ref=${refId}`,
    };
    setLinks((prev) => [newLink, ...prev]);
    setShowCreate(false);
    setForm({ amount: "", description: "", customerEmail: "" });
  };

  const copyUrl = (url: string) => navigator.clipboard.writeText(url);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Payment Links</h1>
          <p className="mt-1 text-sm text-slate-500">Create and share payment links with customers</p>
        </div>
        <button
          onClick={() => setShowCreate(!showCreate)}
          className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg hover:shadow-xl transition"
        >
          + Create Link
        </button>
      </div>

      {showCreate && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">New Payment Link</h2>
          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Amount (INR)</label>
              <input type="number" value={form.amount} onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="5000" required min={1} />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Description</label>
              <input value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="What is this for?" required />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Customer Email (optional)</label>
              <input type="email" value={form.customerEmail} onChange={(e) => setForm((f) => ({ ...f, customerEmail: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="customer@example.com" />
            </div>
          </div>
          <div className="mt-4 flex gap-3">
            <button type="submit" className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg hover:shadow-xl transition">Create Link</button>
            <button type="button" onClick={() => setShowCreate(false)} className="rounded-xl border-2 border-slate-200 px-5 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-50">Cancel</button>
          </div>
        </form>
      )}

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        <div className="divide-y divide-slate-100">
          {links.map((link) => (
            <div key={link.id} className="p-6 hover:bg-slate-50">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-xs font-semibold text-slate-700">{link.referenceId}</span>
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      link.status === "active" ? "bg-green-100 text-green-700" :
                      link.status === "used" ? "bg-blue-100 text-blue-700" :
                      "bg-slate-100 text-slate-600"
                    }`}>{link.status}</span>
                  </div>
                  <p className="mt-1 text-sm text-slate-500">{link.description}</p>
                  <div className="mt-2 flex items-center gap-4 text-xs text-slate-400">
                    <span className="font-medium text-slate-900">₹{link.amount.toLocaleString("en-IN")}</span>
                    <span>Created: {link.createdAt}</span>
                    <span>Expires: {link.expiresAt}</span>
                  </div>
                  <div className="mt-2 flex items-center gap-2">
                    <code className="rounded-lg bg-slate-100 px-2.5 py-1 font-mono text-xs text-slate-600 truncate max-w-sm">{link.url}</code>
                    <button onClick={() => copyUrl(link.url)} className="text-indigo-600 hover:text-indigo-800">
                      <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>
                    </button>
                  </div>
                </div>
                {link.status === "active" && (
                  <button className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50">Deactivate</button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
