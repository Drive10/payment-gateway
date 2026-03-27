import { PAYMENT_AMOUNT, PAYMENT_NOTE, UPI_ID } from "../lib/payment";

const qrValue = encodeURIComponent(
  `upi://pay?pa=${UPI_ID}&pn=Nova%20Checkout&am=${PAYMENT_AMOUNT}&cu=INR&tn=${PAYMENT_NOTE}`
);

export default function UPIQR() {
  return (
    <div className="rounded-3xl border border-emerald-100 bg-emerald-50/80 p-4 text-center">
      <img
        src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${qrValue}`}
        alt="UPI payment QR code"
        className="mx-auto h-48 w-48 rounded-3xl border border-emerald-100 bg-white p-3 shadow-sm"
      />
      <p className="mt-4 text-sm font-medium text-slate-700">
        Scan with any UPI app
      </p>
      <p className="mt-1 text-xs text-slate-500">
        UPI ID: <span className="font-semibold text-slate-700">{UPI_ID}</span>
      </p>
    </div>
  );
}
