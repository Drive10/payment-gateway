import { useState } from "react";

const CARD_SCHEMES = [
  { label: "Visa", numbers: { cardNumber: "4111111111111111", expiry: "12/28", cvv: "123", cardholder: "Test User" }, color: "#2563eb" },
  { label: "Mastercard", numbers: { cardNumber: "5111111111111111", expiry: "12/28", cvv: "123", cardholder: "Test User" }, color: "#dc2626" },
  { label: "Amex", numbers: { cardNumber: "371111111111111", expiry: "12/28", cvv: "1234", cardholder: "Test User" }, color: "#2563eb" },
  { label: "RuPay", numbers: { cardNumber: "6011111111111111", expiry: "12/28", cvv: "123", cardholder: "Test User" }, color: "#16a34a" },
];

const OUTCOMES = [
  { label: "Success", prefix: "4111", color: "#16a34a", desc: "Standard success" },
  { label: "Failure", prefix: "4000", color: "#dc2626", desc: "Card declined" },
  { label: "3DS", prefix: "4002", color: "#d97706", desc: "3D Secure challenge" },
  { label: "OTP", prefix: "4003", color: "#7c3aed", desc: "OTP verification" },
];

const QUICK_AMOUNTS = [100, 500, 1000, 2500, 5000, 10000];

const PAYMENT_METHODS = [
  { id: "card", label: "Card", color: "#4338ca" },
  { id: "upi", label: "UPI", color: "#0891b2" },
  { id: "netbanking", label: "NetBanking", color: "#d97706" },
  { id: "wallet", label: "Wallet", color: "#7c3aed" },
];

const NET_BANKING_PRESETS = [
  { label: "SBI", bankCode: "sbi" },
  { label: "HDFC", bankCode: "hdfc" },
  { label: "ICICI", bankCode: "icici" },
  { label: "Axis", bankCode: "axis" },
];

const WALLET_PRESETS = [
  { label: "Paytm", wallet: "paytm" },
  { label: "PhonePe", wallet: "phonepe" },
  { label: "GPay", wallet: "googlepay" },
  { label: "Amazon", wallet: "amazon" },
];

export function DevTestPanel({ onFillValues, onSetMethod, onSetAmount, onQuickPay }: {
  onFillValues: (vals: Record<string, string>) => void;
  onSetMethod: (m: string) => void;
  onSetAmount: (a: string) => void;
  onQuickPay: (method: string, amount: string, extras?: Record<string, string>) => void;
}) {
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState<"cards" | "methods" | "amounts">("cards");

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        className="fixed bottom-4 right-4 z-50 flex items-center gap-1.5 rounded-full bg-indigo-600 px-3 py-2 text-xs font-semibold text-white shadow-lg hover:bg-indigo-700 transition-all"
      >
        <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
        Dev Tools
      </button>
    );
  }

  const fillCard = (scheme: typeof CARD_SCHEMES[number]) => {
    onFillValues(scheme.numbers);
    onSetMethod("card");
    setTab("cards");
  };

  const triggerOutcome = (prefix: string) => {
    const scheme = CARD_SCHEMES.find(s => s.numbers.cardNumber.startsWith(prefix));
    if (scheme) fillCard(scheme);
  };

  return (
    <div className="fixed bottom-4 right-4 z-50 w-80 rounded-xl border border-slate-200 bg-white shadow-2xl overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2.5">
        <div className="flex items-center gap-2">
          <svg className="h-4 w-4 text-indigo-200" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
          <span className="text-sm font-semibold text-white">Dev Tools</span>
        </div>
        <button onClick={() => setOpen(false)} className="text-white/70 hover:text-white">
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200">
        {(["cards", "methods", "amounts"] as const).map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`flex-1 py-2 text-xs font-medium transition-colors ${
              tab === t ? "border-b-2 border-indigo-600 text-indigo-700" : "text-slate-500 hover:text-slate-700"
            }`}
          >
            {t === "cards" ? "Cards" : t === "methods" ? "Methods" : "Amounts"}
          </button>
        ))}
      </div>

      <div className="max-h-96 overflow-y-auto p-3 space-y-3">
        {/* ── Cards Tab ── */}
        {tab === "cards" && (
          <>
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">Card Schemes</p>
              <div className="grid grid-cols-2 gap-1.5">
                {CARD_SCHEMES.map(scheme => (
                  <button
                    key={scheme.label}
                    onClick={() => fillCard(scheme)}
                    className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700 hover:border-slate-300 hover:bg-slate-50 transition-all"
                  >
                    <span className="h-3 w-5 rounded-sm" style={{ backgroundColor: scheme.color }} />
                    {scheme.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">Payment Outcomes</p>
              <div className="grid grid-cols-2 gap-1.5">
                {OUTCOMES.map(o => (
                  <button
                    key={o.label}
                    onClick={() => triggerOutcome(o.prefix)}
                    className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700 hover:border-slate-300 hover:bg-slate-50 transition-all"
                  >
                    <span className="h-2 w-2 rounded-full" style={{ backgroundColor: o.color }} />
                    <span>{o.label}</span>
                    <span className="ml-auto text-[9px] text-slate-400">{o.desc}</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="rounded-lg bg-slate-50 border border-slate-200 p-2.5">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">Quick Fill Preview</p>
              <div className="text-[10px] text-slate-600 font-mono leading-5">
                <div>Card: •••• •••• •••• 1111</div>
                <div>Exp: 12/28 · CVV: 123</div>
                <div>Holder: Test User</div>
              </div>
            </div>
          </>
        )}

        {/* ── Methods Tab ── */}
        {tab === "methods" && (
          <>
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">Switch Method</p>
              <div className="grid grid-cols-2 gap-1.5">
                {PAYMENT_METHODS.map(m => (
                  <button
                    key={m.id}
                    onClick={() => onSetMethod(m.id)}
                    className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700 hover:border-slate-300 hover:bg-slate-50 transition-all"
                  >
                    {m.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">Net Banking Quick Select</p>
              <div className="flex flex-wrap gap-1.5">
                {NET_BANKING_PRESETS.map(b => (
                  <button
                    key={b.bankCode}
                    onClick={() => { onSetMethod("netbanking"); onFillValues({ bankCode: b.bankCode }); }}
                    className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-[11px] font-medium text-slate-700 hover:border-amber-300 hover:bg-amber-50 transition-all"
                  >
                    {b.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">Wallet Quick Select</p>
              <div className="flex flex-wrap gap-1.5">
                {WALLET_PRESETS.map(w => (
                  <button
                    key={w.wallet}
                    onClick={() => { onSetMethod("wallet"); onFillValues({ wallet: w.wallet }); }}
                    className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-[11px] font-medium text-slate-700 hover:border-purple-300 hover:bg-purple-50 transition-all"
                  >
                    {w.label}
                  </button>
                ))}
              </div>
            </div>
          </>
        )}

        {/* ── Amounts Tab ── */}
        {tab === "amounts" && (
          <>
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">Quick Amounts (₹)</p>
              <div className="grid grid-cols-3 gap-1.5">
                {QUICK_AMOUNTS.map(a => (
                  <button
                    key={a}
                    onClick={() => onSetAmount(String(a))}
                    className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700 hover:border-emerald-300 hover:bg-emerald-50 transition-all"
                  >
                    ₹{a.toLocaleString()}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">One-Click Payments</p>
              <div className="space-y-1.5">
                <button
                  onClick={() => onQuickPay("card", "500", { cardNumber: "4111111111111111", expiry: "12/28", cvv: "123", cardholder: "Test User" })}
                  className="w-full flex items-center gap-2 rounded-lg bg-indigo-50 border border-indigo-200 px-3 py-2 text-xs font-medium text-indigo-700 hover:bg-indigo-100 transition-all"
                >
                  <span className="h-2 w-2 rounded-full bg-indigo-500" />
                  Pay ₹500 via Card (Visa)
                </button>
                <button
                  onClick={() => onQuickPay("upi", "1000")}
                  className="w-full flex items-center gap-2 rounded-lg bg-cyan-50 border border-cyan-200 px-3 py-2 text-xs font-medium text-cyan-700 hover:bg-cyan-100 transition-all"
                >
                  <span className="h-2 w-2 rounded-full bg-cyan-500" />
                  Pay ₹1,000 via UPI
                </button>
                <button
                  onClick={() => onQuickPay("netbanking", "2500", { bankCode: "hdfc" })}
                  className="w-full flex items-center gap-2 rounded-lg bg-amber-50 border border-amber-200 px-3 py-2 text-xs font-medium text-amber-700 hover:bg-amber-100 transition-all"
                >
                  <span className="h-2 w-2 rounded-full bg-amber-500" />
                  Pay ₹2,500 via HDFC NetBanking
                </button>
                <button
                  onClick={() => onQuickPay("wallet", "1500", { wallet: "phonepe" })}
                  className="w-full flex items-center gap-2 rounded-lg bg-purple-50 border border-purple-200 px-3 py-2 text-xs font-medium text-purple-700 hover:bg-purple-100 transition-all"
                >
                  <span className="h-2 w-2 rounded-full bg-purple-500" />
                  Pay ₹1,500 via PhonePe Wallet
                </button>
              </div>
            </div>

            <div className="rounded-lg bg-slate-50 border border-slate-200 p-2.5">
              <div className="flex items-center justify-between text-[10px] text-slate-500">
                <span>Amount set: ₹{QUICK_AMOUNTS[0].toLocaleString()}</span>
                <button
                  onClick={() => { localStorage.clear(); sessionStorage.clear(); }}
                  className="text-red-500 hover:text-red-700 font-medium"
                >
                  Clear Storage
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
