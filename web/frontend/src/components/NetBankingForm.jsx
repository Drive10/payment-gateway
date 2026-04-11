import { memo } from "react";

const banks = [
  { id: "sbi", name: "State Bank of India", icon: "🏦" },
  { id: "hdfc", name: "HDFC Bank", icon: "🏛️" },
  { id: "icici", name: "ICICI Bank", icon: "🔵" },
  { id: "axis", name: "Axis Bank", icon: "🟡" },
  { id: "kotak", name: "Kotak Mahindra", icon: "🟠" },
  { id: "yesbank", name: "YES Bank", icon: "🟢" },
  { id: "pnb", name: "Punjab National Bank", icon: "🔴" },
  { id: "other", name: "Other Banks", icon: "🏦" },
];

export default memo(function NetBankingForm({ values, errors, onChange }) {
  return (
    <div className="space-y-4">
      <div className="rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 p-6 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-indigo-100">
          <svg className="h-8 w-8 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
          </svg>
        </div>
        <h4 className="text-lg font-semibold text-slate-900">Pay with Net Banking</h4>
        <p className="mt-1 text-sm text-slate-600">Select your bank to continue</p>
      </div>

      <div className="grid grid-cols-2 gap-3">
        {banks.map((bank) => (
          <button
            key={bank.id}
            type="button"
            onClick={() => onChange("bankCode", bank.id)}
            className={`flex items-center gap-3 rounded-xl border-2 p-4 transition-all ${
              values.bankCode === bank.id
                ? "border-indigo-500 bg-indigo-50"
                : "border-slate-200 hover:border-slate-300"
            }`}
          >
            <span className="text-xl">{bank.icon}</span>
            <span className="text-sm font-medium text-slate-900">{bank.name}</span>
          </button>
        ))}
      </div>

      {!values.bankCode && (
        <p className="text-center text-sm text-slate-500">Select a bank to continue</p>
      )}
    </div>
  );
});