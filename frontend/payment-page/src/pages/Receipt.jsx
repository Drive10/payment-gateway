import { useCallback } from "react";
import { useLocation, Link } from "react-router-dom";
import { getStoredTransaction } from "../lib/payment";

export default function Receipt() {
  const location = useLocation();
  const receipt = location.state?.transaction ?? getStoredTransaction();

  const downloadPDF = useCallback(async () => {
    if (!receipt) {
      return;
    }

    const jsPDF = (await import("jspdf")).default;

    const doc = new jsPDF();

    doc.setFontSize(18);
    doc.text("Nova Commerce Receipt", 20, 20);
    doc.setFontSize(12);
    doc.text(`Payment ID: ${receipt.id}`, 20, 40);
    doc.text(`Order Ref: ${receipt.orderReference}`, 20, 50);
    doc.text(`Amount: ${receipt.amountLabel}`, 20, 60);
    doc.text(`Method: ${receipt.methodLabel}`, 20, 70);
    doc.text(`Status: ${receipt.status}`, 20, 80);
    doc.text(`Customer: ${receipt.customerLabel}`, 20, 90);
    doc.text(`Date: ${receipt.dateLabel}`, 20, 100);
    if (receipt.correlationId) {
      doc.text(`Correlation ID: ${receipt.correlationId}`, 20, 110);
    }

    doc.save(`${receipt.id}.pdf`);
  }, [receipt]);

  if (!receipt) {
    return (
      <main className="flex min-h-screen items-center justify-center px-4">
        <section className="w-full max-w-md rounded-[2rem] border border-slate-200 bg-white p-8 text-center shadow-[0_20px_80px_rgba(15,23,42,0.12)]">
          <h1 className="text-2xl font-semibold text-slate-950">
            No receipt available
          </h1>
          <p className="mt-3 text-sm text-slate-500">
            Start a new payment to generate a downloadable receipt.
          </p>
          <Link
            to="/"
            className="mt-6 inline-flex rounded-[1.25rem] bg-slate-950 px-5 py-3 font-semibold text-white"
          >
            Back to checkout
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <section className="w-full max-w-4xl overflow-hidden rounded-[2rem] border border-white/60 bg-white/95 shadow-[0_28px_90px_rgba(15,23,42,0.16)] backdrop-blur">
        <div className="grid gap-0 lg:grid-cols-[0.9fr_1.1fr]">
          <div className="bg-[linear-gradient(180deg,#0f172a,#111827)] p-8 text-white">
            <p className="text-xs uppercase tracking-[0.3em] text-cyan-300">
              Receipt
            </p>
            <h1 className="mt-3 text-3xl font-semibold tracking-tight">
              Payment settlement summary
            </h1>
            <p className="mt-4 text-sm leading-6 text-slate-300">
              This receipt includes payment metadata that helps operators trace
              the request through the gateway, payment service, and ledger flow.
            </p>
            <div className="mt-8 rounded-[1.5rem] border border-white/10 bg-white/5 p-5">
              <p className="text-[11px] uppercase tracking-[0.22em] text-slate-400">
                Status
              </p>
              <p className="mt-2 text-2xl font-semibold text-white">
                {receipt.status}
              </p>
            </div>
          </div>

          <div className="p-8">
            <div className="grid gap-4">
              {[
                ["Transaction ID", receipt.id],
                ["Order Ref", receipt.orderReference],
                ["Provider Order ID", receipt.providerOrderId ?? "Pending"],
                ["Provider Payment ID", receipt.providerPaymentId ?? "Pending"],
                ["Amount", receipt.amountLabel],
                ["Method", receipt.methodLabel],
                ["Mode", receipt.environmentLabel ?? receipt.transactionMode],
                ["Customer", receipt.customerLabel],
                ["Date", receipt.dateLabel],
                [
                  "Correlation ID",
                  receipt.correlationId ?? "Assigned by backend",
                ],
              ].map(([label, value]) => (
                <div
                  key={label}
                  className="flex items-center justify-between gap-4 rounded-[1.25rem] border border-slate-200 bg-slate-50 px-4 py-4"
                >
                  <span className="text-sm font-medium text-slate-500">
                    {label}
                  </span>
                  <span className="max-w-[60%] text-right text-sm font-semibold text-slate-950">
                    {value}
                  </span>
                </div>
              ))}
            </div>

            <div className="mt-8">
              <button
                type="button"
                onClick={downloadPDF}
                className="w-full rounded-[1.25rem] bg-slate-950 px-5 py-3 font-semibold text-white transition hover:bg-slate-800"
              >
                Download PDF
              </button>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
