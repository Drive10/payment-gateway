import {
  detectCardBrand,
  formatCardNumber,
  formatExpiry,
  isValidCardNumber,
} from "../lib/payment";

function Field({ label, hint, error, children }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-semibold text-slate-200">
        {label}
      </span>
      {children}
      <p className={`mt-2 text-xs ${error ? "text-rose-300" : "text-slate-400"}`}>
        {error || hint}
      </p>
    </label>
  );
}

export default function CardForm({ values, errors, onChange }) {
  const cardBrand = detectCardBrand(values.cardNumber);
  const isCardComplete = isValidCardNumber(values.cardNumber);

  return (
    <div className="space-y-5 rounded-[1.75rem] border border-white/10 bg-slate-950/70 p-5 shadow-[inset_0_1px_0_rgba(255,255,255,0.04)]">
      <div className="rounded-[1.5rem] border border-cyan-400/20 bg-[linear-gradient(135deg,rgba(14,116,144,0.8),rgba(15,23,42,0.92))] p-5 text-white">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.32em] text-cyan-200">
              Saved card lane
            </p>
            <p className="mt-3 text-xl font-semibold tracking-tight">
              Merchant tokenized checkout
            </p>
          </div>
          <span className="rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-cyan-100">
            {cardBrand}
          </span>
        </div>
        <div className="mt-8 font-mono text-2xl tracking-[0.28em] text-white/90">
          {values.cardNumber || "**** **** **** ****"}
        </div>
        <div className="mt-5 flex items-end justify-between gap-4 text-sm text-cyan-100/90">
          <div>
            <p className="text-[11px] uppercase tracking-[0.2em] text-cyan-200/70">
              Cardholder
            </p>
            <p className="mt-1 font-semibold">
              {values.cardholder || "Your name"}
            </p>
          </div>
          <div className="text-right">
            <p className="text-[11px] uppercase tracking-[0.2em] text-cyan-200/70">
              Expires
            </p>
            <p className="mt-1 font-semibold">{values.expiry || "MM/YY"}</p>
          </div>
        </div>
      </div>

      <Field
        label="Card number"
        error={errors.cardNumber}
        hint={
          isCardComplete
            ? "Card number looks valid."
            : "Use a 16-digit test number like 4242 4242 4242 4242."
        }
      >
        <input
          className="w-full rounded-2xl border border-white/10 bg-slate-900/90 px-4 py-3 text-slate-50 outline-none transition placeholder:text-slate-500 focus:border-cyan-400 focus:ring-4 focus:ring-cyan-400/10"
          inputMode="numeric"
          placeholder="4242 4242 4242 4242"
          value={values.cardNumber}
          onChange={(event) =>
            onChange("cardNumber", formatCardNumber(event.target.value))
          }
          maxLength={19}
          aria-invalid={Boolean(errors.cardNumber)}
        />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Expiry" error={errors.expiry} hint="Enter expiry as MM/YY.">
          <input
            className="w-full rounded-2xl border border-white/10 bg-slate-900/90 px-4 py-3 text-slate-50 outline-none transition placeholder:text-slate-500 focus:border-cyan-400 focus:ring-4 focus:ring-cyan-400/10"
            inputMode="numeric"
            placeholder="MM/YY"
            value={values.expiry}
            onChange={(event) =>
              onChange("expiry", formatExpiry(event.target.value))
            }
            maxLength={5}
            aria-invalid={Boolean(errors.expiry)}
          />
        </Field>

        <Field
          label="CVV"
          error={errors.cvv}
          hint="Three digits on the back of the card."
        >
          <input
            className="w-full rounded-2xl border border-white/10 bg-slate-900/90 px-4 py-3 text-slate-50 outline-none transition placeholder:text-slate-500 focus:border-cyan-400 focus:ring-4 focus:ring-cyan-400/10"
            inputMode="numeric"
            placeholder="123"
            value={values.cvv}
            onChange={(event) =>
              onChange("cvv", event.target.value.replace(/\D/g, "").slice(0, 3))
            }
            maxLength={3}
            aria-invalid={Boolean(errors.cvv)}
          />
        </Field>
      </div>

      <Field
        label="Cardholder name"
        error={errors.cardholder}
        hint="This is used for the receipt and risk checks."
      >
        <input
          className="w-full rounded-2xl border border-white/10 bg-slate-900/90 px-4 py-3 text-slate-50 outline-none transition placeholder:text-slate-500 focus:border-cyan-400 focus:ring-4 focus:ring-cyan-400/10"
          placeholder="Jordan Lee"
          value={values.cardholder}
          onChange={(event) => onChange("cardholder", event.target.value)}
          aria-invalid={Boolean(errors.cardholder)}
        />
      </Field>
    </div>
  );
}
