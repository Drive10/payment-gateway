import { memo } from "react";

const wallets = [
  {
    id: "paytm",
    name: "Paytm",
    color: "#00BAF2",
    gradient: "from-[#00BAF2] to-[#002970]",
    balance: 12450,
  },
  {
    id: "amazon",
    name: "Amazon Pay",
    color: "#FF9900",
    gradient: "from-[#FF9900] to-[#232F3E]",
    balance: 3200,
  },
  {
    id: "phonepe",
    name: "PhonePe",
    color: "#5F259F",
    gradient: "from-[#5F259F] to-[#9B30FF]",
    balance: 8750,
  },
  {
    id: "googlepay",
    name: "Google Pay",
    color: "#4285F4",
    gradient: "from-[#4285F4] to-[#34A853]",
    balance: 5600,
  },
  {
    id: "mobikwik",
    name: "MobiKwik",
    color: "#9C27B0",
    gradient: "from-[#9C27B0] to-[#E040FB]",
    balance: 2100,
  },
  {
    id: "freecharge",
    name: "FreeCharge",
    color: "#FF6F00",
    gradient: "from-[#FF6F00] to-[#FFAB00]",
    balance: 980,
  },
  {
    id: "olamoney",
    name: "Ola Money",
    color: "#05C167",
    gradient: "from-[#05C167] to-[#0288D1]",
    balance: 15000,
  },
  {
    id: "airtel",
    name: "Airtel Payments Bank",
    color: "#E30613",
    gradient: "from-[#E30613] to-[#FF6B6B]",
    balance: 4300,
  },
];

function formatBalance(amount: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount);
}

export default memo(function WalletForm({ values, onChange, amount }) {
  const insufficientFunds = (walletId: string) => {
    const wallet = wallets.find(w => w.id === walletId);
    return wallet ? wallet.balance < (parseFloat(amount) || 0) : false;
  };

  return (
    <div className="space-y-4">
      <div className="rounded-xl border-2 border-slate-200 bg-white overflow-hidden">
        <div className="bg-gradient-to-r from-purple-600 to-purple-700 p-4 text-white">
          <div className="flex items-center gap-3 mb-3">
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3-3v8a3 3 0 003 3z" />
            </svg>
            <div>
              <h4 className="font-semibold">Pay with Wallet</h4>
              <p className="text-xs text-purple-200">Instant payment, no card needed</p>
            </div>
          </div>
          <div className="flex gap-1.5">
            <span className="px-2 py-0.5 bg-white/20 rounded text-xs">Instant Settlement</span>
            <span className="px-2 py-0.5 bg-white/20 rounded text-xs">Zero Fee</span>
          </div>
        </div>

        <div className="p-4">
          <p className="text-xs font-medium text-slate-500 mb-3 uppercase tracking-wider">
            Choose Wallet
          </p>
          <div className="space-y-2 max-h-[300px] overflow-y-auto">
            {wallets.map((wallet) => {
              const insufficient = amount ? insufficientFunds(wallet.id) : false;
              return (
                <button
                  key={wallet.id}
                  type="button"
                  disabled={insufficient}
                  onClick={() => onChange("wallet", wallet.id)}
                  className={`w-full flex items-center gap-3 rounded-xl border-2 p-3 transition-all ${
                    values?.wallet === wallet.id
                      ? "border-purple-500 bg-purple-50 shadow-sm"
                      : insufficient
                      ? "border-red-100 bg-red-50 opacity-60 cursor-not-allowed"
                      : "border-transparent hover:border-slate-200 hover:bg-slate-50"
                  }`}
                >
                  <div className={`h-10 w-10 rounded-lg bg-gradient-to-br ${wallet.gradient} flex items-center justify-center text-white text-xs font-bold shadow-sm`}>
                    {wallet.name.split(" ").map(w => w[0]).join("").slice(0, 2)}
                  </div>
                  <div className="flex-1 text-left">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-slate-900">{wallet.name}</span>
                      {insufficient && (
                        <span className="text-[10px] font-medium text-red-600 bg-red-100 px-1.5 py-0.5 rounded">Low Balance</span>
                      )}
                    </div>
                    <span className="text-xs text-slate-500">Balance: {formatBalance(wallet.balance)}</span>
                  </div>
                  <span className="text-xs text-slate-400">INR</span>
                  {values?.wallet === wallet.id && (
                    <svg className="h-5 w-5 text-purple-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                  )}
                </button>
              );
            })}
          </div>

          {values?.wallet && !insufficientFunds(values.wallet) && (
            <div className="mt-4 p-3 bg-gradient-to-r from-purple-50 to-purple-100 rounded-lg border border-purple-200">
              <div className="flex justify-between items-center">
                <div>
                  <p className="text-xs text-purple-700 font-medium">Selected Wallet</p>
                  <p className="text-sm font-semibold text-purple-900">
                    {wallets.find(w => w.id === values.wallet)?.name}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-purple-700 font-medium">Available Balance</p>
                  <p className="text-sm font-semibold text-purple-900">
                    {formatBalance(wallets.find(w => w.id === values.wallet)?.balance || 0)}
                  </p>
                </div>
              </div>
              <div className="mt-2 pt-2 border-t border-purple-200">
                <div className="flex justify-between text-xs text-purple-700">
                  <span>Amount to Pay</span>
                  <span className="font-semibold">{formatBalance(parseFloat(amount) || 0)}</span>
                </div>
                <div className="flex justify-between text-xs text-purple-700 mt-1">
                  <span>Balance After Payment</span>
                  <span className="font-semibold">
                    {formatBalance((wallets.find(w => w.id === values.wallet)?.balance || 0) - (parseFloat(amount) || 0))}
                  </span>
                </div>
              </div>
            </div>
          )}

          {!values?.wallet && (
            <p className="text-center text-xs text-slate-500 mt-3">
              Select a wallet with sufficient balance to continue
            </p>
          )}
        </div>
      </div>
    </div>
  );
});
