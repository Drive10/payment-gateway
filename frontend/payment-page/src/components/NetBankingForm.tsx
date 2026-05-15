import { memo, useState } from "react";

const banks = [
  { id: "sbi", name: "State Bank of India", color: "#1a5276" },
  { id: "hdfc", name: "HDFC Bank", color: "#e87225" },
  { id: "icici", name: "ICICI Bank", color: "#af1e2c" },
  { id: "axis", name: "Axis Bank", color: "#97144d" },
  { id: "kotak", name: "Kotak Mahindra", color: "#e30613" },
  { id: "yesbank", name: "YES Bank", color: "#1a1a2e" },
  { id: "pnb", name: "Punjab National Bank", color: "#e0432b" },
  { id: "idbi", name: "IDBI Bank", color: "#a6192e" },
  { id: "canara", name: "Canara Bank", color: "#1a6b47" },
  { id: "union", name: "Union Bank of India", color: "#584faf" },
];

function BankLogo({ bank }: { bank: typeof banks[number] }) {
  const initial = bank.name.charAt(0);
  return (
    <svg width="40" height="40" viewBox="0 0 40 40">
      <rect width="40" height="40" rx="8" fill={bank.color} />
      <rect y="28" width="40" height="5" fill="rgba(255,255,255,0.2)" rx="2" />
      <rect x="12" y="8" width="16" height="16" rx="2" fill="rgba(255,255,255,0.3)" />
      <text x="20" y="22" textAnchor="middle" fill="white" fontSize="14" fontWeight="bold">{initial}</text>
    </svg>
  );
}

export default memo(function NetBankingForm({ values, onChange }) {
  const [search, setSearch] = useState("");

  const filtered = banks.filter(b =>
    b.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-4">
      <div className="rounded-xl border-2 border-slate-200 bg-white overflow-hidden">
        <div className="bg-gradient-to-r from-indigo-600 to-indigo-700 p-4 text-white">
          <div className="flex items-center gap-3 mb-3">
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2-2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
            </svg>
            <div>
              <h4 className="font-semibold">Net Banking</h4>
              <p className="text-xs text-indigo-200">Pay directly from your bank account</p>
            </div>
          </div>
          <div className="flex gap-1.5">
            <span className="px-2 py-0.5 bg-white/20 rounded text-xs">No KYC</span>
            <span className="px-2 py-0.5 bg-white/20 rounded text-xs">Instant</span>
            <span className="px-2 py-0.5 bg-white/20 rounded text-xs">₹10L limit</span>
          </div>
        </div>

        <div className="p-4">
          <div className="relative mb-4">
            <svg className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              placeholder="Search your bank..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>

          <p className="text-xs font-medium text-slate-500 mb-3 uppercase tracking-wider">Popular Banks</p>
          <div className="space-y-1 max-h-[260px] overflow-y-auto">
            {filtered.map((bank) => (
              <button
                key={bank.id}
                type="button"
                onClick={() => onChange("bankCode", bank.id)}
                className={`w-full flex items-center gap-3 rounded-lg border-2 p-3 transition-all ${
                  values?.bankCode === bank.id
                    ? "border-indigo-500 bg-indigo-50 shadow-sm"
                    : "border-transparent hover:border-slate-200 hover:bg-slate-50"
                }`}
              >
                <BankLogo bank={bank} />
                <span className="flex-1 text-left">
                  <span className="block text-sm font-medium text-slate-900">{bank.name}</span>
                  <span className="block text-xs text-slate-500">Net Banking</span>
                </span>
                {values?.bankCode === bank.id && (
                  <svg className="h-5 w-5 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                  </svg>
                )}
              </button>
            ))}
          </div>

          {values?.bankCode && (
            <div className="mt-4 p-3 bg-indigo-50 rounded-lg border border-indigo-100">
              <p className="text-xs font-medium text-indigo-700 mb-2">Select Account Type</p>
              <div className="flex gap-2">
                {["SAVINGS", "CURRENT"].map(type => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => onChange("accountType", type)}
                    className={`flex-1 py-2 px-3 rounded-lg text-xs font-medium border transition-all ${
                      values?.accountType === type
                        ? "border-indigo-500 bg-white text-indigo-700 shadow-sm"
                        : "border-slate-200 bg-white text-slate-600 hover:border-slate-300"
                    }`}
                  >
                    {type.charAt(0) + type.slice(1).toLowerCase()}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
});
