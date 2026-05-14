import { lazy, Suspense } from "react";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { ToastProvider } from "./components/ToastProvider";
import { AuthProvider } from "./contexts/AuthContext";
import { ThemeProvider } from "./contexts/ThemeContext";
import DashboardLayout from "./components/layout/DashboardLayout";

const Checkout = lazy(() => import("./pages/Checkout"));
const Processing = lazy(() => import("./pages/Processing"));
const Receipt = lazy(() => import("./pages/Receipt"));
const Success = lazy(() => import("./pages/Success"));
const Failure = lazy(() => import("./pages/Failure"));
const ThreeDsChallenge = lazy(() => import("./pages/ThreeDsChallenge"));

const MerchantLogin = lazy(() => import("./pages/merchant/Login"));
const MerchantRegister = lazy(() => import("./pages/merchant/Register"));
const MerchantDashboard = lazy(() => import("./pages/merchant/Dashboard"));
const Transactions = lazy(() => import("./pages/merchant/Transactions"));
const Customers = lazy(() => import("./pages/merchant/Customers"));
const Refunds = lazy(() => import("./pages/merchant/Refunds"));
const Subscriptions = lazy(() => import("./pages/merchant/Subscriptions"));
const Invoices = lazy(() => import("./pages/merchant/Invoices"));
const ApiKeys = lazy(() => import("./pages/merchant/ApiKeys"));
const Webhooks = lazy(() => import("./pages/merchant/Webhooks"));
const Settlements = lazy(() => import("./pages/merchant/Settlements"));
const PaymentLinks = lazy(() => import("./pages/merchant/PaymentLinks"));
const Plans = lazy(() => import("./pages/merchant/Plans"));
const Disputes = lazy(() => import("./pages/merchant/Disputes"));

const SuspenseFallback = () => (
  <main className="flex min-h-screen items-center justify-center px-4">
    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600" />
  </main>
);

export default function App() {
  return (
    <ToastProvider>
      <ThemeProvider>
      <AuthProvider>
        <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          <Suspense fallback={<SuspenseFallback />}>
            <Routes>
              <Route path="/" element={<Checkout />} />
              <Route path="/processing" element={<Processing />} />
              <Route path="/success" element={<Success />} />
              <Route path="/failure" element={<Failure />} />
              <Route path="/receipt" element={<Receipt />} />
              <Route path="/3ds/challenge" element={<ThreeDsChallenge />} />

              <Route path="/merchant/login" element={<MerchantLogin />} />
              <Route path="/merchant/register" element={<MerchantRegister />} />
              <Route path="/merchant" element={<DashboardLayout />}>
                <Route index element={<MerchantDashboard />} />
                <Route path="transactions" element={<Transactions />} />
                <Route path="customers" element={<Customers />} />
                <Route path="refunds" element={<Refunds />} />
                <Route path="subscriptions" element={<Subscriptions />} />
                <Route path="invoices" element={<Invoices />} />
                <Route path="api-keys" element={<ApiKeys />} />
                <Route path="webhooks" element={<Webhooks />} />
                <Route path="settlements" element={<Settlements />} />
                <Route path="payment-links" element={<PaymentLinks />} />
                <Route path="plans" element={<Plans />} />
                <Route path="disputes" element={<Disputes />} />
              </Route>

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
      </ThemeProvider>
    </ToastProvider>
  );
}
