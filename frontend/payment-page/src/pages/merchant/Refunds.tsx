import { useState } from "react";
import { merchantApi } from "../../lib/merchant";
import toast from "react-hot-toast";

export default function Refunds() {
  const [paymentId, setPaymentId] = useState("");
  const [amount, setAmount] = useState("");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [refunds, setRefunds] = useState<Array<{ id: string; paymentId: string; amount: number; status: string; reason?: string; createdAt: string }>>([]);
  const [fetching, setFetching] = useState(true);

  const handleRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const result = await merchantApi.transactions.refund(paymentId, amount ? parseFloat(amount) : undefined, reason || undefined);
      toast.success("Refund processed successfully");
      setRefunds((prev) => [result, ...prev]);
      setPaymentId("");
      setAmount("");
      setReason("");
    } catch (err: unknown) {
      toast.error((err as Error).message || "Refund failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Refunds</h1>
        <p className="mt-1 text-sm text-slate-500">Process refunds for captured payments</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={handleRefund} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">New Refund</h2>
          <div className="space-y-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Payment ID</label>
              <input value={paymentId} onChange={(e) => setPaymentId(e.target.value)} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="Enter payment ID" required />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Amount (optional - partial refund)</label>
              <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="Leave empty for full refund" />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Reason</label>
              <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={2} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" placeholder="Reason for refund" />
            </div>
            <button type="submit" disabled={loading} className="w-full rounded-xl bg-gradient-to-r from-orange-500 to-red-500 py-2.5 text-sm font-bold text-white shadow-lg transition hover:shadow-xl disabled:opacity-50">
              {loading ? "Processing..." : "Process Refund"}
            </button>
          </div>
        </form>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Recent Refunds</h2>
          {refunds.length === 0 ? (
            <div className="text-center py-12">
              <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M16 15v-1a4 4 0 00-4-4H8m0 0l3 3m-3-3l3-3m9 14V5a2 2 0 00-2-2H6a2 2 0 00-2 2v16l4-2 4 2 4-2 4 2z" />
              </svg>
              <p className="mt-4 text-sm text-slate-500">No refunds processed yet</p>
            </div>
          ) : (
            <div className="space-y-3">
              {refunds.map((r) => (
                <div key={r.id} className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs text-slate-600">{r.id?.slice(0, 12)}...</span>
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${r.status === "COMPLETED" ? "bg-green-100 text-green-700" : "bg-yellow-100 text-yellow-700"}`}>{r.status}</span>
                  </div>
                  <p className="mt-1 text-sm font-medium text-slate-900">₹{(r.amount || 0).toLocaleString("en-IN")}</p>
                  {r.reason && <p className="text-xs text-slate-500">{r.reason}</p>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
