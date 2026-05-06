import { memo, useMemo } from "react";
import { detectCardBrand, formatCardNumber, formatExpiry, isValidCardNumber } from "../lib/payment";

const cardBrands = {
  Visa: { color: "from-[#1a1f71] to-[#2d3a8c]", pattern: /^4/, logo: "VISA" },
  Mastercard: { color: "from-[#eb001b] to-[#f79e1b]", pattern: /^5[1-5]/, logo: "Mastercard" },
  Amex: { color: "from-[#006fcf] to-[#0099df]", pattern: /^3[47]/, logo: "AMEX" },
  RuPay: { color: "from-[#84bc44] to-[#009144]", pattern: /^6/, logo: "RuPay" },
  Default: { color: "from-slate-700 to-slate-900", logo: "" },
};

function getCardBrandData(brand) {
  return cardBrands[brand] || cardBrands.Default;
}

function Field({ label, hint, error, children }) {
  return (
    <div>
      <label className="mb-1 block text-sm font-medium text-slate-700">{label}</label>
      {children}
      <p className={`mt-1 text-xs ${error ? "text-red-500" : "text-slate-500"}`}>
        {error || hint}
      </p>
    </div>
  );
}

const FieldMemo = memo(Field);

export default memo(function CardForm({ values, errors, onChange }) {
  const cardBrand = useMemo(() => detectCardBrand(values.cardNumber), [values.cardNumber]);
  const isCardComplete = useMemo(() => isValidCardNumber(values.cardNumber), [values.cardNumber]);

  const brandData = getCardBrandData(cardBrand);

  return (
    <div className="space-y-4">
      <div className={`relative overflow-hidden rounded-2xl bg-gradient-to-br ${brandData.color} p-6 text-white shadow-2xl`}>
        <div className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-white/10"></div>
        <div className="absolute -bottom-12 -left-8 h-24 w-24 rounded-full bg-white/5"></div>
        
        <div className="relative">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="h-10 w-14 rounded bg-gradient-to-br from-[#c8b568] to-[#e6d982] p-px">
                <div className="flex h-full flex-col rounded bg-gradient-to-br from-[#d4c778] to-[#f0e5a3]">
                  <div className="flex-1"></div>
                  <div className="h-1/2 rounded-b-sm bg-[#bfa54a]"></div>
                </div>
              </div>
              <svg className="h-8" viewBox="0 0 48 48" fill="none">
                <circle cx="24" cy="24" r="20" fill="#fff" fillOpacity="0.15"/>
                <path d="M19 32l5-8 5 8M19 24l5 8 5-8" stroke="#fff" strokeWidth="2" strokeLinecap="round"/>
              </svg>
            </div>
            <span className="rounded-md bg-white/20 px-3 py-1 text-xs font-bold tracking-widest text-white/90">
              {brandData.logo}
            </span>
          </div>
          
          <p className="mt-8 font-mono text-2xl tracking-[0.2em] text-white/95">
            {values.cardNumber || "•••• •••• •••• ••••"}
          </p>
          
          <div className="mt-8 flex items-end justify-between">
            <div>
              <p className="text-[10px] uppercase tracking-wider text-white/60">Card Holder</p>
              <p className="mt-1 font-medium tracking-wide text-white/90">
                {values.cardholder || "YOUR NAME"}
              </p>
            </div>
            <div className="text-right">
              <p className="text-[10px] uppercase tracking-wider text-white/60">Expires</p>
              <p className="mt-1 font-medium tracking-wide text-white/90">
                {values.expiry || "MM/YY"}
              </p>
            </div>
          </div>
        </div>
      </div>

      <FieldMemo
        label="Card Number"
        error={errors.cardNumber}
        hint={isCardComplete ? "✓ Card number is valid" : "Enter 16-digit card number"}
      >
        <div className="relative">
          <div className="absolute left-4 top-1/2 -translate-y-1/2">
            <svg className="h-5 w-5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
            </svg>
          </div>
          <input
            className={`w-full rounded-xl border-2 bg-white px-12 py-3.5 pr-12 font-mono text-slate-900 outline-none transition shadow-sm ${
              errors.cardNumber
                ? "border-red-300 focus:border-red-500 focus:ring-4 focus:ring-red-100"
                : isCardComplete
                ? "border-green-400 focus:border-green-500 focus:ring-4 focus:ring-green-100"
                : "border-slate-200 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-50"
            }`}
            inputMode="numeric"
            placeholder="4242 4242 4242 4242"
            value={values.cardNumber}
            onChange={(event) => onChange("cardNumber", formatCardNumber(event.target.value))}
            maxLength={19}
          />
          <div className="absolute right-4 top-1/2 -translate-y-1/2">
            {isCardComplete ? (
              <svg className="h-5 w-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            ) : null}
          </div>
        </div>
      </FieldMemo>

      <div className="grid grid-cols-2 gap-4">
        <FieldMemo label="Expiry Date" error={errors.expiry} hint="MM/YY format">
          <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2">
              <svg className="h-5 w-5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
            <input
              className={`w-full rounded-xl border-2 bg-white px-12 py-3.5 font-mono text-slate-900 outline-none transition shadow-sm ${
                errors.expiry ? "border-red-300 focus:border-red-500 focus:ring-4 focus:ring-red-100" : "border-slate-200 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-50"
              }`}
              inputMode="numeric"
              placeholder="MM/YY"
              value={values.expiry}
              onChange={(event) => onChange("expiry", formatExpiry(event.target.value))}
              maxLength={5}
            />
          </div>
        </FieldMemo>

        <FieldMemo label="CVV" error={errors.cvv} hint="3 digits on card back">
          <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2">
              <svg className="h-5 w-5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </div>
            <input
              className={`w-full rounded-xl border-2 bg-white px-12 py-3.5 font-mono text-slate-900 outline-none transition shadow-sm ${
                errors.cvv ? "border-red-300 focus:border-red-500 focus:ring-4 focus:ring-red-100" : "border-slate-200 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-50"
              }`}
              inputMode="numeric"
              type="password"
              placeholder="•••"
              value={values.cvv}
              onChange={(event) => onChange("cvv", event.target.value.replace(/\D/g, "").slice(0, 3))}
              maxLength={3}
            />
          </div>
        </FieldMemo>
      </div>

      <FieldMemo
        label="Cardholder Name"
        error={errors.cardholder}
        hint="Name as shown on card"
      >
        <div className="relative">
          <div className="absolute left-4 top-1/2 -translate-y-1/2">
            <svg className="h-5 w-5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </div>
          <input
            className={`w-full rounded-xl border-2 bg-white px-12 py-3.5 text-slate-900 outline-none transition shadow-sm ${
              errors.cardholder ? "border-red-300 focus:border-red-500 focus:ring-4 focus:ring-red-100" : "border-slate-200 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-50"
            }`}
            placeholder="Jordan Lee"
            value={values.cardholder}
            onChange={(event) => onChange("cardholder", event.target.value)}
          />
        </div>
      </FieldMemo>

      <div className="flex items-center justify-center gap-3 rounded-xl border border-green-200 bg-green-50/80 p-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-green-100">
          <svg className="h-5 w-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        <div>
          <p className="text-sm font-semibold text-green-800">Secure Payment</p>
          <p className="text-xs text-green-700">Your card details are encrypted and never stored</p>
        </div>
      </div>
    </div>
  );
});
