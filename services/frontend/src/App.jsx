import { lazy, Suspense } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";

const Checkout = lazy(() => import("./pages/Checkout"));
const Processing = lazy(() => import("./pages/Processing"));
const Receipt = lazy(() => import("./pages/Receipt"));
const Success = lazy(() => import("./pages/Success"));

export default function App() {
  return (
    <BrowserRouter>
      <Suspense
        fallback={
          <main className="flex min-h-screen items-center justify-center px-4">
            <div className="rounded-[2rem] border border-white/60 bg-white/85 px-8 py-6 text-sm font-medium text-slate-500 shadow-[0_20px_80px_rgba(15,23,42,0.14)] backdrop-blur">
              Loading checkout experience...
            </div>
          </main>
        }
      >
        <Routes>
          <Route path="/" element={<Checkout />} />
          <Route path="/processing" element={<Processing />} />
          <Route path="/success" element={<Success />} />
          <Route path="/receipt" element={<Receipt />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
