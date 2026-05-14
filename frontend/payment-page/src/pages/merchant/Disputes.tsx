import { useState } from "react";

interface Dispute {
  id: string;
  paymentId: string;
  amount: number;
  currency: string;
  reason: string;
  status: string;
  filedAt: string;
  deadline: string;
  customerEmail: string;
}

const mockDisputes: Dispute[] = [
  { id: "dsp_001", paymentId: "pay_abc123", amount: 2999, currency: "INR", reason: "Product not received", status: "needs_response", filedAt: "2026-05-10", deadline: "2026-06-09", customerEmail: "customer@example.com" },
  { id: "dsp_002", paymentId: "pay_def456", amount: 1500, currency: "INR", reason: "Duplicate charge", status: "under_review", filedAt: "2026-05-08", deadline: "2026-06-07", customerEmail: "user@test.com" },
  { id: "dsp_003", paymentId: "pay_ghi789", amount: 5000, currency: "INR", reason: "Service not rendered", status: "won", filedAt: "2026-04-20", deadline: "2026-05-20", customerEmail: "biz@corp.com" },
  { id: "dsp_004", paymentId: "pay_jkl012", amount: 750, currency: "INR", reason: "Product defective", status: "lost", filedAt: "2026-04-15", deadline: "2026-05-15", customerEmail: "buyer@shop.com" },
];

export default function Disputes() {
  const [filter, setFilter] = useState("");

  const filtered = mockDisputes.filter((d) => !filter || d.status === filter);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Disputes</h1>
        <p className="mt-1 text-sm text-slate-500">Manage chargebacks and disputes</p>
      </div>

      <div className="grid gap-6 sm:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-500">Total</p>
          <p className="mt-1 text-2xl font-bold text-slate-900">{mockDisputes.length}</p>
        </div>
        <div className="rounded-2xl border border-red-200 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-500">Needs Response</p>
          <p className="mt-1 text-2xl font-bold text-red-600">{mockDisputes.filter((d) => d.status === "needs_response").length}</p>
        </div>
        <div className="rounded-2xl border border-yellow-200 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-500">Under Review</p>
          <p className="mt-1 text-2xl font-bold text-yellow-600">{mockDisputes.filter((d) => d.status === "under_review").length}</p>
        </div>
        <div className="rounded-2xl border border-green-200 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-500">Won</p>
          <p className="mt-1 text-2xl font-bold text-green-600">{mockDisputes.filter((d) => d.status === "won").length}</p>
        </div>
      </div>

      <div className="flex gap-4">
        <select value={filter} onChange={(e) => setFilter(e.target.value)} className="rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none bg-white">
          <option value="">All Status</option>
          <option value="needs_response">Needs Response</option>
          <option value="under_review">Under Review</option>
          <option value="won">Won</option>
          <option value="lost">Lost</option>
        </select>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
                <th className="px-6 py-3">Dispute ID</th>
                <th className="px-6 py-3">Payment</th>
                <th className="px-6 py-3 text-right">Amount</th>
                <th className="px-6 py-3">Reason</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3">Filed</th>
                <th className="px-6 py-3">Deadline</th>
                <th className="px-6 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.map((d) => (
                <tr key={d.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 font-mono text-xs text-slate-700">{d.id}</td>
                  <td className="px-6 py-4 font-mono text-xs text-slate-700">{d.paymentId}</td>
                  <td className="px-6 py-4 text-right font-medium text-slate-900">₹{d.amount.toLocaleString("en-IN")}</td>
                  <td className="px-6 py-4 text-xs text-slate-600 max-w-[150px] truncate">{d.reason}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${
                      d.status === "needs_response" ? "bg-red-100 text-red-700" :
                      d.status === "under_review" ? "bg-yellow-100 text-yellow-700" :
                      d.status === "won" ? "bg-green-100 text-green-700" :
                      "bg-slate-100 text-slate-600"
                    }`}>{d.status.replace("_", " ")}</span>
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-500">{d.filedAt}</td>
                  <td className="px-6 py-4 text-xs text-slate-500">{d.deadline}</td>
                  <td className="px-6 py-4">
                    {d.status === "needs_response" && (
                      <button className="rounded-lg bg-indigo-50 px-3 py-1.5 text-xs font-medium text-indigo-700 hover:bg-indigo-100">Respond</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
        <p className="text-xs text-amber-800">
          <strong>Note:</strong> Dispute management requires a chargeback engine with evidence submission workflows. Currently showing demo data.
        </p>
      </div>
    </div>
  );
}
