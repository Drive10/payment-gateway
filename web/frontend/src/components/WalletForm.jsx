import { memo } from "react";

const wallets = [
  { id: "paytm", name: "Paytm", icon: "🟢" },
  { id: "amazon", name: "Amazon Pay", icon: "🟠" },
  { id: "mobikwik", name: "MobiKwik", icon: "🟣" },
  { id: "freecharge", name: "FreeCharge", icon: "🟡" },
];

export default memo(function WalletForm({ values, onChange }) {
  const isWalletSelected = values && values.wallet;
  
  return (
    <div className="space-y-4">
      <div className="rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 p-6 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-purple-100">
          <svg className="h-8 w-8 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3-3v8a3 3 0 003 3z" />
          </svg>
        </div>
        <h4 className="text-lg font-semibold text-slate-900">Pay with Wallet</h4>
        <p className="mt-1 text-sm text-slate-600">Select your preferred wallet</p>
      </div>

      <div className="grid grid-cols-2 gap-3">
        {wallets.map((wallet) => (
          <button
            key={wallet.id}
            type="button"
            onClick={() => onChange("wallet", wallet.id)}
            className={`flex items-center gap-3 rounded-xl border-2 p-4 transition-all ${
              values.wallet === wallet.id
                ? "border-purple-500 bg-purple-50"
                : "border-slate-200 hover:border-slate-300"
            }`}
          >
            <span className="text-2xl">{wallet.icon}</span>
            <span className="font-medium text-slate-900">{wallet.name}</span>
          </button>
        ))}
      </div>

      {!values.wallet && (
        <p className="text-center text-sm text-slate-500">Select a wallet to continue</p>
      )}
    </div>
  );
});