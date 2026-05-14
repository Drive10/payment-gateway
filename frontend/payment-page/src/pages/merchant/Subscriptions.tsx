import { useState, useEffect } from "react";
import { merchantApi } from "../../lib/merchant";
import toast from "react-hot-toast";

interface Sub {
  subscriptionId: string;
  customerEmail: string;
  customerName: string;
  planName: string;
  amount: number;
  currency: string;
  status: string;
  interval: string;
  currentPeriodEnd: string;
  createdAt: string;
}

export default function Subscriptions() {
  const [subs, setSubs] = useState<Sub[]>([]);
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ planId: "", customerEmail: "", customerName: "" });

  useEffect(() => {
    merchantApi.subscriptions.list()
      .then((data) => setSubs(Array.isArray(data) ? data : []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await merchantApi.subscriptions.create(form);
      toast.success("Subscription created");
      setShowCreate(false);
      merchantApi.subscriptions.list().then((data) => setSubs(Array.isArray(data) ? data : []));
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  };

  const handleAction = async (id: string, action: "cancel" | "pause" | "resume") => {
    try {
      await merchantApi.subscriptions[action](id);
      toast.success(`Subscription ${action}ed`);
      merchantApi.subscriptions.list().then((data) => setSubs(Array.isArray(data) ? data : []));
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  };

  const filtered = subs.filter((s) => !filter || s.status === filter);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Subscriptions</h1>
          <p className="mt-1 text-sm text-slate-500">{subs.length} total</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg hover:shadow-xl transition">
          + New Subscription
        </button>
      </div>

      {showCreate && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Create Subscription</h2>
          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Plan ID</label>
              <input value={form.planId} onChange={(e) => setForm((f) => ({ ...f, planId: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="plan-id" required />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Customer Email</label>
              <input type="email" value={form.customerEmail} onChange={(e) => setForm((f) => ({ ...f, customerEmail: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="customer@example.com" required />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Customer Name</label>
              <input value={form.customerName} onChange={(e) => setForm((f) => ({ ...f, customerName: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="Customer name" />
            </div>
          </div>
          <div className="mt-4 flex gap-3">
            <button type="submit" className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-bold text-white">Create</button>
            <button type="button" onClick={() => setShowCreate(false)} className="rounded-xl border-2 border-slate-200 px-5 py-2.5 text-sm font-medium text-slate-600">Cancel</button>
          </div>
        </form>
      )}

      <div className="flex gap-4">
        <select value={filter} onChange={(e) => setFilter(e.target.value)} className="rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none bg-white">
          <option value="">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="PAUSED">Paused</option>
          <option value="PAST_DUE">Past Due</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="TRIALING">Trial</option>
        </select>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-32"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" /></div>
      ) : (
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="px-6 py-3">Customer</th>
                  <th className="px-6 py-3">Plan</th>
                  <th className="px-6 py-3 text-right">Amount</th>
                  <th className="px-6 py-3">Interval</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Next Billing</th>
                  <th className="px-6 py-3">Created</th>
                  <th className="px-6 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.length === 0 ? (
                  <tr><td colSpan={8} className="px-6 py-12 text-center text-sm text-slate-500">No subscriptions</td></tr>
                ) : (
                  filtered.map((s) => (
                    <tr key={s.subscriptionId} className="hover:bg-slate-50">
                      <td className="px-6 py-4">
                        <p className="font-medium text-slate-900">{s.customerName || s.customerEmail}</p>
                        {s.customerName && <p className="text-xs text-slate-500">{s.customerEmail}</p>}
                      </td>
                      <td className="px-6 py-4 text-slate-700">{s.planName}</td>
                      <td className="px-6 py-4 text-right font-medium text-slate-900">₹{(s.amount || 0).toLocaleString("en-IN")}</td>
                      <td className="px-6 py-4 text-slate-600 capitalize">{s.interval}</td>
                      <td className="px-6 py-4">
                        <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${
                          s.status === "ACTIVE" || s.status === "TRIALING" ? "bg-green-100 text-green-700" :
                          s.status === "PAUSED" ? "bg-yellow-100 text-yellow-700" :
                          s.status === "PAST_DUE" ? "bg-red-100 text-red-700" :
                          "bg-slate-100 text-slate-600"
                        }`}>{s.status}</span>
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-500">{s.currentPeriodEnd ? new Date(s.currentPeriodEnd).toLocaleDateString("en-IN") : "-"}</td>
                      <td className="px-6 py-4 text-xs text-slate-500">{s.createdAt ? new Date(s.createdAt).toLocaleDateString("en-IN") : "-"}</td>
                      <td className="px-6 py-4">
                        <div className="flex gap-2">
                          {s.status === "ACTIVE" && <button onClick={() => handleAction(s.subscriptionId, "pause")} className="text-xs font-medium text-yellow-600 hover:text-yellow-800">Pause</button>}
                          {s.status === "PAUSED" && <button onClick={() => handleAction(s.subscriptionId, "resume")} className="text-xs font-medium text-green-600 hover:text-green-800">Resume</button>}
                          {(s.status === "ACTIVE" || s.status === "PAUSED" || s.status === "PAST_DUE") && <button onClick={() => handleAction(s.subscriptionId, "cancel")} className="text-xs font-medium text-red-600 hover:text-red-800">Cancel</button>}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
