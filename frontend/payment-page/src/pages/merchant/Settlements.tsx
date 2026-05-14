import { useState, useEffect } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { merchantApi } from "../../lib/merchant";

interface Settlement {
  id?: string;
  batchId?: string;
  amount?: number;
  currency?: string;
  status?: string;
  createdAt?: string;
  completedAt?: string;
  payoutReference?: string;
}

export default function Settlements() {
  const { user } = useAuth();
  const [balance, setBalance] = useState<{ available?: number; pending?: number; settled?: number } | null>(null);
  const [settlements, setSettlements] = useState<Settlement[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    Promise.allSettled([
      merchantApi.finance.balance(user.id).catch(() => null),
      merchantApi.finance.settlements(user.id).catch(() => null),
    ]).then(([bal, s]) => {
      if (bal.status === "fulfilled" && bal.value) setBalance(bal.value);
      if (s.status === "fulfilled" && s.value) setSettlements(Array.isArray(s.value) ? s.value : []);
      setLoading(false);
    });
  }, [user]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Settlements</h1>
        <p className="mt-1 text-sm text-slate-500">Track your merchant settlements and payouts</p>
      </div>

      <div className="grid gap-6 sm:grid-cols-3">
        <div className="rounded-2xl border border-green-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-green-500 p-3">
              <svg className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" /></svg>
            </div>
            <div>
              <p className="text-sm text-slate-500">Available Balance</p>
              <p className="text-2xl font-bold text-slate-900">₹{(balance?.available ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-yellow-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-yellow-500 p-3">
              <svg className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            </div>
            <div>
              <p className="text-sm text-slate-500">Pending Settlement</p>
              <p className="text-2xl font-bold text-slate-900">₹{(balance?.pending ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-blue-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-blue-500 p-3">
              <svg className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>
            </div>
            <div>
              <p className="text-sm text-slate-500">Total Settled</p>
              <p className="text-2xl font-bold text-slate-900">₹{(balance?.settled ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Settlement Batches</h2>
        </div>
        {settlements.length === 0 ? (
          <div className="px-6 py-12 text-center">
            <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <p className="mt-4 text-sm text-slate-500">No settlement batches found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="px-6 py-3">Batch ID</th>
                  <th className="px-6 py-3 text-right">Amount</th>
                  <th className="px-6 py-3">Currency</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Created</th>
                  <th className="px-6 py-3">Completed</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {settlements.map((s) => (
                  <tr key={s.id || s.batchId} className="hover:bg-slate-50">
                    <td className="px-6 py-4 font-mono text-xs text-slate-700">{(s.id || s.batchId || "").slice(0, 12)}...</td>
                    <td className="px-6 py-4 text-right font-medium text-slate-900">₹{(s.amount || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}</td>
                    <td className="px-6 py-4 text-slate-600">{s.currency || "INR"}</td>
                    <td className="px-6 py-4">
                      <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${
                        s.status === "COMPLETED" ? "bg-green-100 text-green-700" :
                        s.status === "PENDING" || s.status === "APPROVED" ? "bg-yellow-100 text-yellow-700" :
                        s.status === "PROCESSING" ? "bg-blue-100 text-blue-700" :
                        "bg-red-100 text-red-700"
                      }`}>{s.status}</span>
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500">{s.createdAt ? new Date(s.createdAt).toLocaleDateString("en-IN") : "-"}</td>
                    <td className="px-6 py-4 text-xs text-slate-500">{s.completedAt ? new Date(s.completedAt).toLocaleDateString("en-IN") : "-"}</td>
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
