import {
  detectCardBrand,
  formatCardNumber,
  formatExpiry,
  isValidCardNumber,
} from "../lib/payment";

export default function CardForm({ values, errors, onChange }) {
  const cardBrand = detectCardBrand(values.cardNumber);
  const isCardComplete = isValidCardNumber(values.cardNumber);

  return (
    <div className="space-y-4 rounded-3xl border border-slate-200 bg-slate-50/80 p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-slate-900">Card details</p>
          <p className="mt-1 text-xs text-slate-500">
            Test-friendly flow with live formatting and inline validation.
          </p>
        </div>
        <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 shadow-sm">
          {cardBrand}
        </span>
      </div>

      <label className="block">
        <span className="mb-2 block text-sm font-medium text-slate-700">
          Card number
        </span>
        <input
          className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
          inputMode="numeric"
          placeholder="4242 4242 4242 4242"
          value={values.cardNumber}
          onChange={(event) =>
            onChange("cardNumber", formatCardNumber(event.target.value))
          }
          maxLength={19}
          aria-invalid={Boolean(errors.cardNumber)}
        />
        {errors.cardNumber ? (
          <p className="mt-2 text-sm text-rose-600">{errors.cardNumber}</p>
        ) : (
          <p className="mt-2 text-xs text-slate-500">
            {isCardComplete
              ? "Card number looks good."
              : "Use any 16-digit test card number."}
          </p>
        )}
      </label>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">
            Expiry
          </span>
          <input
            className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
            inputMode="numeric"
            placeholder="MM/YY"
            value={values.expiry}
            onChange={(event) =>
              onChange("expiry", formatExpiry(event.target.value))
            }
            maxLength={5}
            aria-invalid={Boolean(errors.expiry)}
          />
          {errors.expiry && (
            <p className="mt-2 text-sm text-rose-600">{errors.expiry}</p>
          )}
        </label>

        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">
            CVV
          </span>
          <input
            className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
            inputMode="numeric"
            placeholder="123"
            value={values.cvv}
            onChange={(event) =>
              onChange("cvv", event.target.value.replace(/\D/g, "").slice(0, 3))
            }
            maxLength={3}
            aria-invalid={Boolean(errors.cvv)}
          />
          {errors.cvv && (
            <p className="mt-2 text-sm text-rose-600">{errors.cvv}</p>
          )}
        </label>
      </div>

      <label className="block">
        <span className="mb-2 block text-sm font-medium text-slate-700">
          Cardholder name
        </span>
        <input
          className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
          placeholder="Jordan Lee"
          value={values.cardholder}
          onChange={(event) => onChange("cardholder", event.target.value)}
          aria-invalid={Boolean(errors.cardholder)}
        />
        {errors.cardholder && (
          <p className="mt-2 text-sm text-rose-600">{errors.cardholder}</p>
        )}
      </label>
    </div>
  );
}
