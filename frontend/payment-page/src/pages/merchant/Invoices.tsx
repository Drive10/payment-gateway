import { useState, useEffect } from "react";
import { merchantApi } from "../../lib/merchant";
import toast from "react-hot-toast";

interface Inv {
  invoiceNumber: string;
  customerEmail: string;
  customerName: string;
  amount: number;
  currency: string;
  status: string;
  description: string;
  dueDate: string;
  createdAt: string;
}

export default function Invoices() {
  const [invoices, setInvoices] = useState<Inv[]>([]);
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ customerEmail: "", customerName: "", amount: "", description: "" });

  useEffect(() => {
    merchantApi.invoices.list()
      .then((data) => setInvoices(Array.isArray(data) ? data : []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await merchantApi.invoices.create({ ...form, amount: parseFloat(form.amount), currency: "INR" });
      toast.success("Invoice created");
      setShowCreate(false);
      setForm({ customerEmail: "", customerName: "", amount: "", description: "" });
      merchantApi.invoices.list().then((data) => setInvoices(Array.isArray(data) ? data : []));
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  };

  const handleAction = async (num: string, action: "send" | "pay" | "cancel") => {
    try {
      await merchantApi.invoices[action](num);
      toast.success(`Invoice ${action}ed`);
      merchantApi.invoices.list().then((data) => setInvoices(Array.isArray(data) ? data : []));
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  };

  const filtered = invoices.filter((inv) => !filter || inv.status === filter);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Invoices</h1>
          <p className="mt-1 text-sm text-slate-500">{invoices.length} total</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg hover:shadow-xl transition">
          + New Invoice
        </button>
      </div>

      {showCreate && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Create Invoice</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Customer Email</label>
              <input type="email" value={form.customerEmail} onChange={(e) => setForm((f) => ({ ...f, customerEmail: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" required />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Customer Name</label>
              <input value={form.customerName} onChange={(e) => setForm((f) => ({ ...f, customerName: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Amount (INR)</label>
              <input type="number" value={form.amount} onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" required />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Description</label>
              <input value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" />
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
          <option value="DRAFT">Draft</option>
          <option value="PENDING">Pending</option>
          <option value="PAID">Paid</option>
          <option value="OVERDUE">Overdue</option>
          <option value="CANCELLED">Cancelled</option>
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
                  <th className="px-6 py-3">Invoice</th>
                  <th className="px-6 py-3">Customer</th>
                  <th className="px-6 py-3 text-right">Amount</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Due Date</th>
                  <th className="px-6 py-3">Created</th>
                  <th className="px-6 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.length === 0 ? (
                  <tr><td colSpan={7} className="px-6 py-12 text-center text-sm text-slate-500">No invoices</td></tr>
                ) : (
                  filtered.map((inv) => (
                    <tr key={inv.invoiceNumber} className="hover:bg-slate-50">
                      <td className="px-6 py-4 font-mono text-xs text-slate-700">{inv.invoiceNumber}</td>
                      <td className="px-6 py-4">
                        <p className="font-medium text-slate-900">{inv.customerName || inv.customerEmail}</p>
                        {inv.customerName && <p className="text-xs text-slate-500">{inv.customerEmail}</p>}
                      </td>
                      <td className="px-6 py-4 text-right font-medium text-slate-900">₹{(inv.amount || 0).toLocaleString("en-IN")}</td>
                      <td className="px-6 py-4">
                        <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${
                          inv.status === "PAID" ? "bg-green-100 text-green-700" :
                          inv.status === "PENDING" ? "bg-yellow-100 text-yellow-700" :
                          inv.status === "OVERDUE" ? "bg-red-100 text-red-700" :
                          inv.status === "DRAFT" ? "bg-slate-100 text-slate-600" :
                          "bg-slate-100 text-slate-600"
                        }`}>{inv.status}</span>
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-500">{inv.dueDate ? new Date(inv.dueDate).toLocaleDateString("en-IN") : "-"}</td>
                      <td className="px-6 py-4 text-xs text-slate-500">{inv.createdAt ? new Date(inv.createdAt).toLocaleDateString("en-IN") : "-"}</td>
                      <td className="px-6 py-4">
                        <div className="flex gap-2">
                          {inv.status === "DRAFT" && <button onClick={() => handleAction(inv.invoiceNumber, "send")} className="text-xs font-medium text-indigo-600">Send</button>}
                          {inv.status === "PENDING" && <button onClick={() => handleAction(inv.invoiceNumber, "pay")} className="text-xs font-medium text-green-600">Pay</button>}
                          {(inv.status === "DRAFT" || inv.status === "PENDING") && <button onClick={() => handleAction(inv.invoiceNumber, "cancel")} className="text-xs font-medium text-red-600">Cancel</button>}
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
