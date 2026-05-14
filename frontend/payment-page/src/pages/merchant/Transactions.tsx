import { useState, useEffect } from "react";
import { merchantApi } from "../../lib/merchant";

interface Transaction {
  paymentId?: string;
  id?: string;
  orderId: string;
  amount: number;
  currency: string;
  status: string;
  method: string;
  customerEmail?: string;
  customerName?: string;
  createdAt: string;
  failureReason?: string;
  errorCode?: string;
}

const statusColors: Record<string, string> = {
  CREATED: "bg-yellow-100 text-yellow-800",
  AUTHORIZATION_PENDING: "bg-blue-100 text-blue-800",
  AUTHORIZED: "bg-purple-100 text-purple-800",
  CAPTURED: "bg-green-100 text-green-800",
  FAILED: "bg-red-100 text-red-800",
  REFUNDED: "bg-orange-100 text-orange-800",
  CANCELLED: "bg-slate-100 text-slate-800",
};

export default function Transactions() {
  const [txns, setTxns] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  useEffect(() => {
    merchantApi.transactions.list(200, 0)
      .then((data) => setTxns(Array.isArray(data) ? data : data?.payments || data?.data || []))
      .catch(() => setTxns([]))
      .finally(() => setLoading(false));
  }, []);

  const filtered = txns.filter((t) => {
    const matchesSearch = !search ||
      (t.orderId || "").toLowerCase().includes(search.toLowerCase()) ||
      (t.paymentId || t.id || "").toLowerCase().includes(search.toLowerCase()) ||
      (t.customerEmail || "").toLowerCase().includes(search.toLowerCase());
    const matchesStatus = !statusFilter || t.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const statuses = [...new Set(txns.map((t) => t.status))];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Transactions</h1>
          <p className="mt-1 text-sm text-slate-500">{txns.length} total transactions</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-4">
        <div className="flex-1 min-w-[200px]">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by order ID, payment ID, or email..."
            className="w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-50"
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none transition focus:border-indigo-500 bg-white"
        >
          <option value="">All Statuses</option>
          {statuses.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600" />
        </div>
      ) : (
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="px-6 py-3">Payment ID</th>
                  <th className="px-6 py-3">Order ID</th>
                  <th className="px-6 py-3 text-right">Amount</th>
                  <th className="px-6 py-3">Currency</th>
                  <th className="px-6 py-3">Method</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Customer</th>
                  <th className="px-6 py-3">Date</th>
                  <th className="px-6 py-3">Error</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="px-6 py-12 text-center text-sm text-slate-500">No transactions found</td>
                  </tr>
                ) : (
                  filtered.map((txn) => (
                    <tr key={txn.paymentId || txn.id} className="hover:bg-slate-50">
                      <td className="px-6 py-4 font-mono text-xs text-slate-700">{(txn.paymentId || txn.id || "").slice(0, 12)}...</td>
                      <td className="px-6 py-4 font-mono text-xs text-slate-700">{txn.orderId}</td>
                      <td className="px-6 py-4 text-right font-medium text-slate-900">{(txn.amount || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                      <td className="px-6 py-4 text-slate-600">{txn.currency || "INR"}</td>
                      <td className="px-6 py-4 text-slate-600 capitalize">{txn.method?.toLowerCase() || "-"}</td>
                      <td className="px-6 py-4">
                        <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${statusColors[txn.status] || "bg-slate-100 text-slate-700"}`}>
                          {txn.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-500">{txn.customerEmail || txn.customerName || "-"}</td>
                      <td className="px-6 py-4 text-xs text-slate-500">{txn.createdAt ? new Date(txn.createdAt).toLocaleDateString("en-IN") : "-"}</td>
                      <td className="px-6 py-4 text-xs text-red-500 max-w-[120px] truncate" title={txn.failureReason || ""}>
                        {txn.failureReason || txn.errorCode || "-"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <div className="border-t border-slate-100 px-6 py-3 text-xs text-slate-400">
            Showing {filtered.length} of {txns.length} transactions
          </div>
        </div>
      )}
    </div>
  );
}
