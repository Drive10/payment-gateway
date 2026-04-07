import { useState } from "react";
import { UPI_ID } from "../lib/payment";
import { QRCodeSVG } from "qrcode.react";

export default function UpiQR() {
  const [showQR, setShowQR] = useState(false);

  return (
    <div className="space-y-4">
      <div className="rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 p-6 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-cyan-100">
          <svg className="h-8 w-8 text-cyan-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 001 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1-1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
          </svg>
        </div>
        <h4 className="text-lg font-semibold text-slate-900">Pay with UPI</h4>
        <p className="mt-1 text-sm text-slate-600">
          Scan the QR code or use UPI ID to pay
        </p>
        <p className="mt-2 font-mono text-sm font-medium text-cyan-600">{UPI_ID}</p>

        {!showQR ? (
          <button
            onClick={() => setShowQR(true)}
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-cyan-600 px-6 py-3 text-sm font-semibold text-white transition hover:bg-cyan-700"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1-1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1-1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1-1v2a1 1 0 001 1z" />
            </svg>
            Show QR Code
          </button>
        ) : (
          <div className="mt-4">
            <div className="mx-auto">
              <QRCodeSVG
                value={`upi://pay?pa=${UPI_ID}&pn=PayFlow Merchant&am=100&cu=INR`}
                size={180}
                bgColor="white"
                fgColor="black"
                level="H"
                includeMargin={true}
              />
            </div>
            <p className="mt-3 text-xs text-slate-500">
              Scan with any UPI app to complete payment
            </p>
          </div>
        )}
      </div>

      <div className="rounded-xl bg-gradient-to-r from-cyan-50 to-teal-50 p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-cyan-100">
            <svg className="h-5 w-5 text-cyan-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-medium text-slate-900">UPI Payment Instructions</p>
            <ol className="mt-1 text-xs text-slate-600">
              <li>1. Open your UPI app (GPay, PhonePe, Paytm)</li>
              <li>2. Scan the QR code or enter UPI ID</li>
              <li>3. Verify amount and confirm payment</li>
            </ol>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-center gap-4">
        <span className="text-xs text-slate-500">Supported:</span>
        <div className="flex gap-2">
          {["GPay", "PhonePe", "Paytm", "BHIM"].map((app) => (
            <span key={app} className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
              {app}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}
