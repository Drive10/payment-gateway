import { useState, useEffect } from "react";
import { merchantApi } from "../../lib/merchant";
import toast from "react-hot-toast";

interface PaymentMethod {
  token: string;
  type: string;
  cardLast4: string;
  cardBrand: string;
  expiryMonth: number;
  expiryYear: number;
  upiId: string;
  isDefault: boolean;
  createdAt: string;
}

export default function Customers() {
  const [search, setSearch] = useState("");
  const [methods, setMethods] = useState<PaymentMethod[]>([]);
  const [loading, setLoading] = useState(false);
  const [email, setEmail] = useState("");

  const loadMethods = async (customerEmail: string) => {
    setLoading(true);
    try {
      const data = await merchantApi.vault.list(customerEmail);
      setMethods(Array.isArray(data) ? data : []);
    } catch {
      setMethods([]);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (token: string) => {
    try {
      await merchantApi.vault.delete(token);
      toast.success("Payment method removed");
      if (email) loadMethods(email);
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  };

  const handleSetDefault = async (token: string) => {
    try {
      await merchantApi.vault.setDefault(token);
      toast.success("Default payment method updated");
      if (email) loadMethods(email);
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (search) {
      setEmail(search);
      loadMethods(search);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Customers</h1>
        <p className="mt-1 text-sm text-slate-500">Look up customers and their saved payment methods</p>
      </div>

      <form onSubmit={handleSearch} className="flex gap-4">
        <div className="flex-1 max-w-md">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by customer email..."
            className="w-full rounded-xl border-2 border-slate-200 px-4 py-2.5 text-sm outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-50"
          />
        </div>
        <button type="submit" className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-bold text-white">Search</button>
      </form>

      {loading ? (
        <div className="flex items-center justify-center h-32"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" /></div>
      ) : email && methods.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
          <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M17 16v2a2 2 0 01-2 2H5a2 2 0 01-2-2v-2a2 2 0 012-2h10a2 2 0 012 2zm0-8V6a2 2 0 00-2-2H5a2 2 0 00-2 2v2a2 2 0 002 2h10a2 2 0 002-2z" />
          </svg>
          <p className="mt-4 text-sm text-slate-500">No saved payment methods for <strong>{email}</strong></p>
        </div>
      ) : email ? (
        <div className="space-y-4">
          <h2 className="text-lg font-semibold text-slate-900">{email}</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {methods.map((pm) => (
              <div key={pm.token} className={`rounded-2xl border-2 p-5 shadow-sm ${pm.isDefault ? "border-indigo-300 bg-indigo-50" : "border-slate-200 bg-white"}`}>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-500 text-white text-lg font-bold shadow-md">
                      {pm.type === "card" ? "💳" : "📱"}
                    </div>
                    <div>
                      <p className="font-semibold text-slate-900 capitalize">{pm.type}</p>
                      {pm.cardBrand && <p className="text-xs text-slate-500">{pm.cardBrand}</p>}
                    </div>
                  </div>
                  {pm.isDefault && <span className="rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-semibold text-indigo-700">Default</span>}
                </div>
                <div className="mt-4">
                  {pm.type === "card" ? (
                    <p className="font-mono text-lg text-slate-900">•••• {pm.cardLast4}</p>
                  ) : (
                    <p className="font-mono text-sm text-slate-900">{pm.upiId}</p>
                  )}
                </div>
                {pm.type === "card" && pm.expiryMonth && (
                  <p className="mt-1 text-xs text-slate-500">Expires {pm.expiryMonth}/{pm.expiryYear}</p>
                )}
                <div className="mt-4 flex gap-2">
                  {!pm.isDefault && <button onClick={() => handleSetDefault(pm.token)} className="text-xs font-medium text-indigo-600 hover:text-indigo-800">Set Default</button>}
                  <button onClick={() => handleDelete(pm.token)} className="text-xs font-medium text-red-600 hover:text-red-800">Remove</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
          <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
          </svg>
          <p className="mt-4 text-sm text-slate-500">Enter a customer email to view their saved payment methods</p>
        </div>
      )}
    </div>
  );
}
