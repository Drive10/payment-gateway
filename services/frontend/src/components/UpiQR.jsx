import {
  PAYMENT_AMOUNT,
  PAYMENT_NOTE,
  UPI_ID,
  formatCurrency,
} from "../lib/payment";

const qrValue = encodeURIComponent(
  `upi://pay?pa=${UPI_ID}&pn=Nova%20Commerce&am=${PAYMENT_AMOUNT}&cu=INR&tn=${PAYMENT_NOTE}`,
);

export default function UpiQR() {
  return (
    <div className="rounded-[1.75rem] border border-emerald-400/15 bg-[linear-gradient(180deg,rgba(6,78,59,0.92),rgba(15,23,42,0.96))] p-5 text-white">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-emerald-200/80">
            UPI collect
          </p>
          <h3 className="mt-2 text-xl font-semibold tracking-tight">
            Scan to approve in your banking app
          </h3>
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-right">
          <p className="text-[11px] uppercase tracking-[0.2em] text-emerald-100/70">
            Payable
          </p>
          <p className="mt-1 text-lg font-semibold">
            {formatCurrency(PAYMENT_AMOUNT)}
          </p>
        </div>
      </div>

      <div className="mt-5 grid gap-5 md:grid-cols-[220px_1fr] md:items-center">
        <img
          src={`https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${qrValue}`}
          alt="UPI payment QR code"
          className="mx-auto h-56 w-56 rounded-[1.5rem] border border-white/10 bg-white p-3 shadow-[0_18px_48px_rgba(15,23,42,0.38)]"
        />

        <div className="space-y-4">
          <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-4">
            <p className="text-sm font-semibold text-emerald-100">UPI ID</p>
            <p className="mt-1 text-base text-white">{UPI_ID}</p>
          </div>
          <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-4">
            <p className="text-sm font-semibold text-emerald-100">
              Approval steps
            </p>
            <p className="mt-1 text-sm leading-6 text-slate-200">
              Scan the QR, verify the merchant as Nova Commerce, and approve
              the collect request in your UPI app.
            </p>
          </div>
          <p className="text-xs uppercase tracking-[0.18em] text-emerald-100/70">
            Secure intent creation with a backend idempotency key and request
            correlation.
          </p>
        </div>
      </div>
    </div>
  );
}
