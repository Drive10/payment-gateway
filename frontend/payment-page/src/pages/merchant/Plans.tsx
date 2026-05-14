import { useState, useEffect } from "react";
import { merchantApi } from "../../lib/merchant";
import toast from "react-hot-toast";

interface Plan {
  id: string;
  name: string;
  description: string;
  amount: number;
  currency: string;
  interval: string;
  trialPeriodDays: number | null;
  status: string;
  createdAt: string;
}

export default function Plans() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ name: "", description: "", amount: "", currency: "INR", interval: "monthly", trialPeriodDays: "" });

  useEffect(() => {
    merchantApi.plans.list()
      .then((data) => setPlans(Array.isArray(data) ? data : []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await merchantApi.plans.create({
        name: form.name,
        description: form.description,
        amount: parseFloat(form.amount),
        currency: form.currency,
        interval: form.interval,
        trialPeriodDays: form.trialPeriodDays ? parseInt(form.trialPeriodDays) : null,
      });
      toast.success("Plan created");
      setShowCreate(false);
      setForm({ name: "", description: "", amount: "", currency: "INR", interval: "monthly", trialPeriodDays: "" });
      merchantApi.plans.list().then((data) => setPlans(Array.isArray(data) ? data : []));
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  };

  const handleDeactivate = async (id: string) => {
    try {
      await merchantApi.plans.deactivate(id);
      toast.success("Plan deactivated");
      merchantApi.plans.list().then((data) => setPlans(Array.isArray(data) ? data : []));
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Plans</h1>
          <p className="mt-1 text-sm text-slate-500">{plans.length} subscription plans</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg hover:shadow-xl transition">
          + New Plan
        </button>
      </div>

      {showCreate && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Create Plan</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Plan Name</label>
              <input value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" required />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Amount</label>
              <input type="number" value={form.amount} onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" required />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Description</label>
              <input value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" required />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Currency</label>
              <select value={form.currency} onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none bg-white">
                <option value="INR">INR</option>
                <option value="USD">USD</option>
                <option value="EUR">EUR</option>
                <option value="GBP">GBP</option>
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Interval</label>
              <select value={form.interval} onChange={(e) => setForm((f) => ({ ...f, interval: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none bg-white">
                <option value="daily">Daily</option>
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
                <option value="yearly">Yearly</option>
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Trial Days (optional)</label>
              <input type="number" value={form.trialPeriodDays} onChange={(e) => setForm((f) => ({ ...f, trialPeriodDays: e.target.value }))} className="block w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-indigo-500" />
            </div>
          </div>
          <div className="mt-4 flex gap-3">
            <button type="submit" className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-bold text-white">Create Plan</button>
            <button type="button" onClick={() => setShowCreate(false)} className="rounded-xl border-2 border-slate-200 px-5 py-2.5 text-sm font-medium text-slate-600">Cancel</button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-32"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" /></div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {plans.length === 0 ? (
            <div className="col-span-full text-center py-12 text-sm text-slate-500">No plans yet</div>
          ) : (
            plans.map((plan) => (
              <div key={plan.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm hover:shadow-md transition">
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-semibold text-slate-900">{plan.name}</h3>
                    <p className="mt-1 text-sm text-slate-500 line-clamp-2">{plan.description}</p>
                  </div>
                  <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${plan.status === "active" ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-600"}`}>
                    {plan.status}
                  </span>
                </div>
                <div className="mt-4">
                  <p className="text-2xl font-bold text-slate-900">
                    {plan.currency === "INR" ? "₹" : plan.currency}{plan.amount?.toLocaleString("en-IN")}
                    <span className="text-sm font-normal text-slate-500">/{plan.interval}</span>
                  </p>
                </div>
                {plan.trialPeriodDays && <p className="mt-2 text-xs text-slate-500">{plan.trialPeriodDays}-day trial</p>}
                <div className="mt-4 flex gap-2">
                  {plan.status === "active" && (
                    <button onClick={() => handleDeactivate(plan.id)} className="text-xs font-medium text-red-600 hover:text-red-800">Deactivate</button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
