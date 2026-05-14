import { useState, useEffect } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { merchantApi } from "../../lib/merchant";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from "recharts";

interface RecentTxn {
  id: string;
  paymentId?: string;
  orderId: string;
  amount: number;
  currency: string;
  status: string;
  method: string;
  createdAt: string;
  customerEmail?: string;
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

const chartColors = ["#22c55e", "#ef4444", "#f59e0b", "#6366f1", "#14b8a6"];

export default function MerchantDashboard() {
  const { user } = useAuth();
  const [recentTxns, setRecentTxns] = useState<RecentTxn[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    merchantApi.transactions.list(100, 0)
      .then((data) => {
        const list = Array.isArray(data) ? data : [];
        setRecentTxns(list);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const revenue = recentTxns.filter((t) => t.status === "CAPTURED").reduce((s, t) => s + (t.amount || 0), 0);
  const succeeded = recentTxns.filter((t) => t.status === "CAPTURED").length;
  const failed = recentTxns.filter((t) => t.status === "FAILED").length;
  const total = recentTxns.length;

  const statusDistribution = [
    { name: "Captured", value: succeeded },
    { name: "Failed", value: failed },
    { name: "Other", value: total - succeeded - failed },
  ].filter((d) => d.value > 0);

  const last7Days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (6 - i));
    const label = d.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
    const dayTxns = recentTxns.filter((t) => {
      const td = new Date(t.createdAt);
      return td.toLocaleDateString("en-IN") === d.toLocaleDateString("en-IN");
    });
    return {
      date: label,
      revenue: dayTxns.filter((t) => t.status === "CAPTURED").reduce((s, t) => s + (t.amount || 0), 0),
      failed: dayTxns.filter((t) => t.status === "FAILED").reduce((s, t) => s + (t.amount || 0), 0),
    };
  });

  const stats = [
    { label: "Total Revenue", value: `₹${revenue.toLocaleString("en-IN", { maximumFractionDigits: 2 })}`, change: "", positive: true, color: "from-emerald-500 to-teal-500" },
    { label: "Transactions", value: String(total), change: `${succeeded} success`, positive: true, color: "from-blue-500 to-indigo-500" },
    { label: "Success Rate", value: total > 0 ? `${Math.round(succeeded / total * 100)}%` : "0%", change: `${succeeded}/${total}`, positive: true, color: "from-green-500 to-emerald-500" },
    { label: "Failed", value: String(failed), change: total > 0 ? `${Math.round(failed / total * 100)}% rate` : "0%", positive: false, color: "from-red-500 to-rose-500" },
  ];

  if (loading) {
    return <div className="flex items-center justify-center h-64"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600" /></div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
        <p className="mt-1 text-sm text-slate-500">Welcome back, {user?.firstName || "Merchant"}</p>
      </div>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <div key={stat.label} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <div className={`rounded-xl bg-gradient-to-br ${stat.color} p-3 shadow-lg`}>
                <svg className="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
            <p className="mt-4 text-2xl font-bold text-slate-900">{stat.value}</p>
            <p className="mt-1 text-sm text-slate-500">{stat.label}</p>
            <p className="text-xs text-slate-400">{stat.change}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Revenue (Last 7 Days)</h2>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={last7Days}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="#94a3b8" />
              <YAxis tick={{ fontSize: 11 }} stroke="#94a3b8" />
              <Tooltip />
              <Bar dataKey="revenue" name="Revenue" fill="#22c55e" radius={[4, 4, 0, 0]} />
              <Bar dataKey="failed" name="Failed" fill="#ef4444" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Payment Status</h2>
          {statusDistribution.length > 0 ? (
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie data={statusDistribution} cx="50%" cy="50%" innerRadius={60} outerRadius={100} dataKey="value" label>
                  {statusDistribution.map((_, idx) => (
                    <Cell key={idx} fill={chartColors[idx % chartColors.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-[250px] text-sm text-slate-400">No data yet</div>
          )}
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Recent Transactions</h2>
        </div>
        {recentTxns.length === 0 ? (
          <div className="px-6 py-12 text-center">
            <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <p className="mt-4 text-sm text-slate-500">No transactions yet</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="px-6 py-3">Order ID</th>
                  <th className="px-6 py-3 text-right">Amount</th>
                  <th className="px-6 py-3">Method</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Date</th>
                  <th className="px-6 py-3">Customer</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recentTxns.slice(0, 10).map((txn) => (
                  <tr key={txn.id || txn.paymentId} className="hover:bg-slate-50">
                    <td className="px-6 py-4 font-mono text-xs text-slate-700">{txn.orderId?.slice(0, 16)}</td>
                    <td className="px-6 py-4 text-right font-medium text-slate-900">₹{(txn.amount || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}</td>
                    <td className="px-6 py-4 text-slate-600 capitalize">{txn.method?.toLowerCase() || "-"}</td>
                    <td className="px-6 py-4">
                      <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${statusColors[txn.status] || "bg-slate-100 text-slate-700"}`}>{txn.status}</span>
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500">{txn.createdAt ? new Date(txn.createdAt).toLocaleDateString("en-IN") : "-"}</td>
                    <td className="px-6 py-4 text-xs text-slate-500">{txn.customerEmail || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
