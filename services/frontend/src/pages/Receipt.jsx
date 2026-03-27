import jsPDF from "jspdf";
import { Link, useLocation } from "react-router-dom";
import { getStoredTransaction } from "../lib/payment";

export default function Receipt() {
  const location = useLocation();
  const receipt = location.state?.transaction ?? getStoredTransaction();

  const downloadPDF = () => {
    if (!receipt) {
      return;
    }

    const doc = new jsPDF();

    doc.setFontSize(18);
    doc.text("Nova Checkout Receipt", 20, 20);
    doc.setFontSize(12);
    doc.text(`Transaction ID: ${receipt.id}`, 20, 40);
    doc.text(`Amount: ${receipt.amountLabel}`, 20, 50);
    doc.text(`Method: ${receipt.methodLabel}`, 20, 60);
    doc.text(`Status: ${receipt.status}`, 20, 70);
    doc.text(`Customer: ${receipt.customerLabel}`, 20, 80);
    doc.text(`Date: ${receipt.dateLabel}`, 20, 90);

    doc.save(`${receipt.id}.pdf`);
  };

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
            className="mt-6 inline-flex rounded-2xl bg-slate-950 px-5 py-3 font-semibold text-white"
          >
            Back to checkout
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <section className="w-full max-w-lg rounded-[2rem] border border-white/60 bg-white/90 p-8 shadow-[0_20px_80px_rgba(15,23,42,0.14)] backdrop-blur">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm uppercase tracking-[0.2em] text-cyan-700">
              Receipt
            </p>
            <h1 className="mt-2 text-3xl font-semibold text-slate-950">
              Payment summary
            </h1>
          </div>
          <span className="rounded-full bg-emerald-100 px-4 py-2 text-sm font-semibold text-emerald-700">
            {receipt.status}
          </span>
        </div>

        <div className="mt-8 space-y-4 text-sm text-slate-600">
          {[
            ["Transaction ID", receipt.id],
            ["Order Ref", receipt.orderReference],
            ["Amount", receipt.amountLabel],
            ["Method", receipt.methodLabel],
            ["Customer", receipt.customerLabel],
            ["Date", receipt.dateLabel],
          ].map(([label, value]) => (
            <div
              key={label}
              className="flex items-center justify-between gap-4 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-4"
            >
              <span className="font-medium text-slate-500">{label}</span>
              <span className="text-right font-semibold text-slate-900">
                {value}
              </span>
            </div>
          ))}
        </div>

        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <button
            type="button"
            onClick={downloadPDF}
            className="flex-1 rounded-2xl bg-slate-950 px-5 py-3 font-semibold text-white transition hover:bg-slate-800"
          >
            Download PDF
          </button>
          <Link
            to="/"
            className="flex-1 rounded-2xl border border-slate-200 px-5 py-3 text-center font-semibold text-slate-700 transition hover:border-slate-300 hover:text-slate-950"
          >
            New payment
          </Link>
        </div>
      </section>
    </main>
  );
}
