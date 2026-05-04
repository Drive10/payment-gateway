import { memo, useMemo } from "react";
import { detectCardBrand, formatCardNumber, formatExpiry, isValidCardNumber } from "../lib/payment.ts";

const cardBrands = {
  Visa: { color: "from-blue-600 to-blue-800", pattern: /^4/ },
  Mastercard: { color: "from-orange-500 to-red-600", pattern: /^5[1-5]/ },
  Amex: { color: "from-cyan-500 to-blue-600", pattern: /^3[47]/ },
  RuPay: { color: "from-green-500 to-emerald-600", pattern: /^6/ },
};

function getCardBrandClass(brand) {
  return cardBrands[brand]?.color || "from-slate-600 to-slate-800";
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

  return (
    <div className="space-y-4">
      <div className={`rounded-xl bg-gradient-to-br ${getCardBrandClass(cardBrand)} p-5 text-white shadow-lg`}>
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs uppercase tracking-wider text-white/80">Card Number</p>
            <p className="mt-2 font-mono text-xl tracking-wider">
              {values.cardNumber || "•••• •••• •••• ••••"}
            </p>
          </div>
          <div className={`rounded-lg bg-white/20 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-white`}>
            {cardBrand}
          </div>
        </div>
        <div className="mt-6 grid grid-cols-2 gap-4">
          <div>
            <p className="text-xs uppercase tracking-wider text-white/70">Cardholder</p>
            <p className="mt-1 font-medium">
              {values.cardholder || "Your Name"}
            </p>
          </div>
          <div className="text-right">
            <p className="text-xs uppercase tracking-wider text-white/70">Expires</p>
            <p className="mt-1 font-medium">{values.expiry || "MM/YY"}</p>
          </div>
        </div>
      </div>

      <FieldMemo
        label="Card Number"
        error={errors.cardNumber}
        hint={isCardComplete ? "✓ Card number is valid" : "Enter 16-digit card number"}
      >
        <div className="relative">
          <input
            className={`w-full rounded-xl border-2 bg-slate-50 px-4 py-3 pr-12 font-mono text-slate-900 outline-none transition ${
              errors.cardNumber
                ? "border-red-300 focus:border-red-500"
                : isCardComplete
                ? "border-green-300 focus:border-green-500"
                : "border-slate-200 focus:border-cyan-500"
            }`}
            inputMode="numeric"
            placeholder="4242 4242 4242 4242"
            value={values.cardNumber}
            onChange={(event) => onChange("cardNumber", formatCardNumber(event.target.value))}
            maxLength={19}
          />
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            {isCardComplete ? (
              <svg className="h-5 w-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            ) : (
              <svg className="h-5 w-5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
              </svg>
            )}
          </div>
        </div>
      </FieldMemo>

      <div className="grid grid-cols-2 gap-4">
        <FieldMemo label="Expiry Date" error={errors.expiry} hint="MM/YY format">
          <input
            className={`w-full rounded-xl border-2 bg-slate-50 px-4 py-3 font-mono text-slate-900 outline-none transition ${
              errors.expiry ? "border-red-300 focus:border-red-500" : "border-slate-200 focus:border-cyan-500"
            }`}
            inputMode="numeric"
            placeholder="MM/YY"
            value={values.expiry}
            onChange={(event) => onChange("expiry", formatExpiry(event.target.value))}
            maxLength={5}
          />
        </FieldMemo>

        <FieldMemo label="CVV" error={errors.cvv} hint="3 digits on card back">
          <div className="relative">
            <input
              className={`w-full rounded-xl border-2 bg-slate-50 px-4 py-3 font-mono text-slate-900 outline-none transition ${
                errors.cvv ? "border-red-300 focus:border-red-500" : "border-slate-200 focus:border-cyan-500"
              }`}
              inputMode="numeric"
              type="password"
              placeholder="•••"
              value={values.cvv}
              onChange={(event) => onChange("cvv", event.target.value.replace(/\D/g, "").slice(0, 3))}
              maxLength={3}
            />
            <svg className="absolute right-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
        </FieldMemo>
      </div>

      <FieldMemo
        label="Cardholder Name"
        error={errors.cardholder}
        hint="Name as shown on card"
      >
        <input
          className={`w-full rounded-xl border-2 bg-slate-50 px-4 py-3 text-slate-900 outline-none transition ${
            errors.cardholder ? "border-red-300 focus:border-red-500" : "border-slate-200 focus:border-cyan-500"
          }`}
          placeholder="Jordan Lee"
          value={values.cardholder}
          onChange={(event) => onChange("cardholder", event.target.value)}
        />
      </FieldMemo>

      <div className="flex items-center gap-2 rounded-lg bg-slate-100 p-3">
        <svg className="h-5 w-5 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
        <p className="text-xs text-slate-600">
          Your card details are encrypted and never stored on our servers.
        </p>
      </div>
    </div>
  );
});
