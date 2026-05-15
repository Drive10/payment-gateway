import { motion } from "framer-motion";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { formatCurrency, getStoredTransaction, getStoredCardDetails, updateStoredTransaction } from "../lib/payment";
import { OTPModal } from "../components/payment/OTPModal";

const API_BASE_URL = window.__ENV__?.API_BASE_URL || "http://localhost:3001";
const API_ROOT = API_BASE_URL.endsWith("/api/v1") ? API_BASE_URL : `${API_BASE_URL}/api/v1`;
const IS_PRODUCTION = window.__ENV__?.IS_PRODUCTION === true;

const FINAL_STATES = ["CAPTURED", "COMPLETED", "SUCCESS"];
const FAILURE_STATES = ["FAILED", "EXPIRED"];

export default function Processing() {
  const location = useLocation();
  const navigate = useNavigate();
  const checkout = location.state?.checkout ?? getStoredTransaction();
  const [error, setError] = useState(null);
  const [status, setStatus] = useState("processing");
  const [pollAttempt, setPollAttempt] = useState(0);
  const [showOtpModal, setShowOtpModal] = useState(false);
  const [otp, setOtp] = useState("");
  const [otpError, setOtpError] = useState("");
  const [progressMessage, setProgressMessage] = useState("Initializing payment...");

  useEffect(() => {
    if (!checkout?.payment?.id) {
      navigate("/", { replace: true });
      return;
    }

    if (IS_PRODUCTION && checkout.payment.checkoutUrl) {
      window.location.href = checkout.payment.checkoutUrl;
      return;
    }

    // Start polling backend for status
    pollBackendForStatus();
  }, [checkout, navigate]);

  const pollBackendForStatus = async () => {
    if (!checkout?.payment?.id) return;

    const maxAttempts = 30; // 30 attempts * 3s = 90 seconds max

    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        // Use status endpoint which returns progress
        const response = await fetch(
          `${API_ROOT}/payments/${checkout.payment.id}/status`,
          {
            headers: checkout.token ? { Authorization: `Bearer ${checkout.token}` } : {},
          }
        );
        const data = await response.json();

        if (data.success && data.data) {
          const paymentStatus = data.data.status;
          const message = data.data.statusMessage || "Processing...";

          setStatus(paymentStatus);
          setProgressMessage(message);
          setPollAttempt(attempt);

// Auto-capture for simulator/test mode when status is CREATED
          if (paymentStatus === "CREATED") {
            // Test/sandbox mode: process card payment first
            if (!IS_PRODUCTION && checkout.method === "card") {
              setProgressMessage("Processing card payment...");
              
              const cardDetails = location.state?.cardDetails || getStoredCardDetails();
              console.debug('Processing - method:', checkout.method);
              console.debug('Processing - payment.id:', checkout.payment?.id);
              
              if (cardDetails) {
                try {
                  const processResponse = await fetch(
                    `${API_ROOT}/payments/${checkout.payment.id}/process`,
                    {
                      method: 'POST',
                      headers: {
                        'Content-Type': 'application/json',
                        ...(checkout.token ? { Authorization: `Bearer ${checkout.token}` } : {}),
                      },
                      body: JSON.stringify({
                        last4: cardDetails.cardNumber?.slice(-4),
                        expiry: cardDetails.expiry,
                        cardholder: cardDetails.cardholder,
                        token: 'sim_token_' + checkout.payment.id,
                      }),
                    }
                  );
const processData = await processResponse.json();
                    console.debug('Processing - process response:', processData);
                    
                    if (processData.success && processData.data) {
                      const newStatus = processData.data.status;
                      console.debug('Processing - newStatus:', newStatus);
                    
                    if (newStatus === 'AUTHORIZATION_PENDING') {
                      setStatus("AUTHORIZATION_PENDING");
                      setProgressMessage("Awaiting OTP verification");
                      setShowOtpModal(true);
                      return;
                    } else if (newStatus === 'CHALLENGE_REQUIRED') {
                      setStatus("CHALLENGE_REQUIRED");
                      setProgressMessage("Redirecting to 3D Secure...");
                      await new Promise(r => setTimeout(r, 1500));
                      navigate("/3ds/challenge", { replace: true, state: { checkout, transactionId: checkout.payment.id } });
                      return;
                    } else if (newStatus === 'FAILED') {
                      setError(processData.data.message || "Payment failed");
                      setStatus("failed");
                      await new Promise(r => setTimeout(r, 2000));
                      navigate("/failure", { replace: true, state: { transaction: checkout, error: processData.data.message } });
                      return;
                    } else {
                      setStatus(newStatus);
                      setProgressMessage("Payment processed successfully");
                    }
                  }
                } catch (processErr) {
                  console.error('Card processing error:', processErr);
                }
              }
              return;
            }
            // For non-card payments in test mode
            if (!IS_PRODUCTION && checkout.method !== "card") {
              console.debug('Processing UPI - method:', checkout.method, 'payment.id:', checkout.payment?.id);
              setProgressMessage("Processing " + checkout.method + " payment...");
              
              // UPI requires special handling
              if (checkout.method === "upi") {
                console.debug('Processing - entering UPI block');
                try {
                  // Create UPI payment
                  console.debug('Processing - calling upiv2/create, amount:', checkout.amount);
                  const upiResponse = await fetch(
                    `${API_ROOT}/payments/upiv2/create`,
                    {
                      method: "POST",
                      headers: {
                        "Content-Type": "application/json",
                        ...(checkout.token ? { Authorization: `Bearer ${checkout.token}` } : {}),
                      },
                      body: JSON.stringify({
                        amount: checkout.amount,
                        vpa: "test@upi",
                      }),
                    }
                  );
                  const upiData = await upiResponse.json();
                  console.debug('Processing - UPI create response:', JSON.stringify(upiData));
                  
                  if (upiData.success && upiData.data) {
                    const upiTxnId = upiData.data.transactionId;
                    
                    // Poll UPI status
                    console.debug('Processing - polling UPI status, txn:', upiTxnId);
                    for (let i = 0; i < 10; i++) {
                      console.debug('Processing - polling attempt:', i);
                      await new Promise((r) => setTimeout(r, 2000));
                      const statusResponse = await fetch(
                        `${API_ROOT}/payments/upiv2/${upiTxnId}/check-status`,
                        {
                          method: "POST",
                          headers: {
                            "Content-Type": "application/json",
                            ...(checkout.token ? { Authorization: `Bearer ${checkout.token}` } : {}),
                          },
                          body: JSON.stringify({ vpa: "test@upi" }),
                        }
                      );
                      const statusData = await statusResponse.json();
                      console.debug('Processing - UPI status response:', JSON.stringify(statusData));
                      
                      if (statusData.success && statusData.data) {
                        const upiStatus = statusData.data.status;
                        
                        if (upiStatus === "SUCCESS") {
                          setStatus("CAPTURED");
                          setProgressMessage("Payment completed!");
                          navigate("/success", {
                            replace: true,
                            state: {
                              transaction: buildTransaction("CAPTURED"),
                            },
                          });
                          return;
                        } else if (upiStatus === "FAILED") {
                          setStatus("failed");
                          setError("Payment failed");
                          navigate("/failure", {
                            replace: true,
                            state: { transaction: checkout, error: "Payment failed" },
                          });
                          return;
                        }
                      }
                    }
                    
                    // Timeout - payment still pending
                    setStatus("failed");
                    setError("Payment verification timed out");
                    navigate("/failure", {
                      replace: true,
                      state: { transaction: checkout, error: "Payment timeout" },
                    });
                    return;
                  }
                } catch (upiErr) {
                  console.error('UPI error:', upiErr);
                }
                return; // Exit after UPI handling
              }
              
              // ── Net Banking ───────────────────────────────
              if (checkout.method === "netbanking") {
                setProgressMessage("Redirecting to bank portal...");
                await new Promise(r => setTimeout(r, 1500));
                setStatus("redirecting_bank");
                setProgressMessage("Connected to " + (checkout.bankName || "bank"));
                await new Promise(r => setTimeout(r, 1000));
                setProgressMessage("Awaiting authentication...");
                await new Promise(r => setTimeout(r, 1500));

                const headers = { "Content-Type": "application/json", ...(checkout.token ? { Authorization: `Bearer ${checkout.token}` } : {}) };
                const api = (p: string, body?: string) => fetch(`${API_ROOT}/payments${p}`, { method: "POST", headers, body });

                // Transition CREATED → AUTHORIZATION_PENDING → AUTHORIZED → CAPTURED
                await api(`/${checkout.payment.id}/authorize-pending`);
                await new Promise(r => setTimeout(r, 400));
                await api(`/${checkout.payment.id}/authorize`);
                await new Promise(r => setTimeout(r, 400));
                const capRes = await api(`/${checkout.payment.id}/capture`);
                const capData = await capRes.json().catch(() => ({}));
                if (capData?.success || capData?.data?.status === "CAPTURED") {
                  navigate("/success", { replace: true, state: { transaction: buildTransaction("CAPTURED") } });
                  return;
                }
                // Fall through to status polling
                continue;
              }

              // ── Wallet ────────────────────────────────────
              if (checkout.method === "wallet") {
                setProgressMessage("Connecting to " + (checkout.walletName || "wallet") + "...");
                await new Promise(r => setTimeout(r, 1000));
                setProgressMessage("Checking balance...");
                await new Promise(r => setTimeout(r, 800));
                setStatus("pending_wallet");
                setProgressMessage("Enter your wallet PIN to confirm");
                await new Promise(r => setTimeout(r, 1500));

                const headers = { "Content-Type": "application/json", ...(checkout.token ? { Authorization: `Bearer ${checkout.token}` } : {}) };
                const api = (p: string) => fetch(`${API_ROOT}/payments${p}`, { method: "POST", headers });

                await api(`/${checkout.payment.id}/authorize-pending`);
                await new Promise(r => setTimeout(r, 400));
                await api(`/${checkout.payment.id}/authorize`);
                await new Promise(r => setTimeout(r, 400));
                const capRes = await api(`/${checkout.payment.id}/capture`);
                const capData = await capRes.json().catch(() => ({}));
                if (capData?.success || capData?.data?.status === "CAPTURED") {
                  navigate("/success", { replace: true, state: { transaction: buildTransaction("CAPTURED") } });
                  return;
                }
                continue;
              }

              continue;
            }
            setProgressMessage("Waiting for payment provider...");
            await new Promise((r) => setTimeout(r, 3000));
            continue;
          }

          // Check final states
          if (FINAL_STATES.includes(paymentStatus)) {
            navigate("/success", {
              replace: true,
              state: {
                transaction: buildTransaction(paymentStatus),
              },
            });
            return;
          }

          // Check failure states
          if (FAILURE_STATES.includes(paymentStatus)) {
            setError(message);
            setTimeout(() => {
              navigate("/failure", {
                replace: true,
                state: { transaction: checkout, error: message },
              });
            }, 2000);
            return;
          }

          // Check if needs OTP verification
          if (paymentStatus === "AUTHORIZATION_PENDING") {
            setShowOtpModal(true);
            return;
          }

          // Check if needs 3D Secure challenge
          if (paymentStatus === "CHALLENGE_REQUIRED" || data.data?.requires3ds) {
            const challengeUrl = data.data?.threeDsChallengeUrl;
            if (challengeUrl) {
              setProgressMessage("Redirecting to 3D Secure verification...");
              setTimeout(() => {
                window.location.href = challengeUrl;
              }, 1500);
              return;
            }
          }

          // Still processing, wait and poll again
          await new Promise((r) => setTimeout(r, 3000));
        }
      } catch (err) {
        await new Promise((r) => setTimeout(r, 3000));
      }
    }

    // Timeout - payment taking too long
    setError("Payment is taking longer than expected. Please try again or contact support.");
    setTimeout(() => {
      navigate("/failure", {
        replace: true,
        state: { transaction: checkout, error: "Payment timeout" },
      });
    }, 3000);
  };

  const buildTransaction = (status) => ({
    id: checkout.payment.id,
    orderId: checkout.order?.id,
    orderReference: checkout.order?.externalReference,
    status: status,
    amount: checkout.amount,
    amountLabel: formatCurrency(checkout.amount),
    method: checkout.method,
    methodLabel: checkout.method === "upi" ? "UPI" : "Card",
    customerLabel:
      checkout.cardholder ||
      `${checkout.customer?.firstName} ${checkout.customer?.lastName}`.trim(),
    environmentLabel: "Sandbox lane",
    correlationId: checkout.correlationId,
  });

  const handleOtpRequired = () => {
    setShowOtpModal(true);
    setStatus("pending_otp");
  };

  const handleOtpSubmit = async () => {
    if (!otp || otp.length !== 6) {
      setOtpError("Please enter 6-digit OTP");
      return;
    }

    // Test mode: accept any 6-digit or 123456
    if (!IS_PRODUCTION && (otp === "123456" || otp.length === 6)) {
      // Call backend to verify before marking success
      try {
        const response = await fetch(
          `${API_ROOT}/payments/${checkout.payment.id}/verify-otp`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${checkout.token}`,
            },
            body: JSON.stringify({ otp }),
          }
        );
        const data = await response.json();
        if (data.success) {
          const transaction = buildTransaction("CAPTURED");
          updateStoredTransaction({ status: "CAPTURED", ...transaction });
          setStatus("CAPTURED");
          setProgressMessage("Payment completed (test mode)");
          setShowOtpModal(false);
          navigate("/success", {
            replace: true,
            state: { transaction: buildTransaction("CAPTURED") },
          });
          return;
        }
      } catch (err) {
        // Fall through to verify-otp API call
      }
    }

    setOtpError("");
    setStatus("processing");
    setShowOtpModal(false);

    try {
      const response = await fetch(
        `${API_ROOT}/payments/${checkout.payment.id}/verify-otp`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${checkout.token}`,
          },
          body: JSON.stringify({ otp }),
        }
      );
      const data = await response.json();

      if (data.success && data.data?.status === "CAPTURED") {
        navigate("/success", {
          replace: true,
          state: { transaction: buildTransaction("CAPTURED") },
        });
        return;
      }

      // After OTP verify, poll backend for final status
      pollBackendForStatus();
    } catch (err) {
      setOtpError("Verification failed. Please try again.");
      setShowOtpModal(true);
      setStatus("pending_otp");
    }
  };

  const pollPaymentStatus = async (initialData) => {
    setShowOtpModal(false);
    setStatus("processing");

    for (let i = 0; i < 15; i++) {
      try {
        const response = await fetch(
          `${API_ROOT}/payments/${checkout.payment.id}`,
          {
            headers: { Authorization: `Bearer ${checkout.token}` },
          }
        );
        const data = await response.json();

        if (data.success && (data.data?.status === "COMPLETED" || data.data?.status === "CAPTURED")) {
          const transaction = {
            id: checkout.payment.id,
            orderId: checkout.order.id,
            orderReference: checkout.order.externalReference,
            status: data.data.status,
            amount: checkout.amount,
            amountLabel: formatCurrency(checkout.amount),
            method: checkout.method,
    methodLabel: checkout.method === "upi" ? "UPI" : checkout.method === "netbanking" ? (checkout.bankName || "Net Banking") : checkout.method === "wallet" ? (checkout.walletName || "Wallet") : "Card",
            customerLabel: checkout.cardholder || `${checkout.customer?.firstName} ${checkout.customer?.lastName}`.trim(),
            environmentLabel: "Sandbox lane",
            correlationId: checkout.correlationId,
          };
          navigate("/success", { replace: true, state: { transaction } });
          return;
        }

        setPollAttempt(i + 1);
        await new Promise(r => setTimeout(r, 3000));
      } catch {
        await new Promise(r => setTimeout(r, 3000));
      }
    }

    setStatus("failed");
    setError("Payment verification timed out");
    setTimeout(() => {
      navigate("/failure", { replace: true, state: { transaction: checkout, error: "Payment verification timed out" } });
    }, 2000);
  };

  const handleCancel = () => {
    setShowOtpModal(false);
    navigate("/failure", { replace: true, state: { transaction: checkout, error: "Payment cancelled by user" } });
  };

  if (showOtpModal) {
    return (
      <OTPModal
        isOpen={showOtpModal}
        onClose={() => setShowOtpModal(false)}
        onVerify={async (otpValue: string) => {
          setStatus("processing");
          setShowOtpModal(false);
          try {
            const response = await fetch(
              `${API_ROOT}/payments/${checkout.payment.id}/verify-otp`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                  Authorization: `Bearer ${checkout.token}`,
                },
                body: JSON.stringify({ otp: otpValue }),
              }
            );
            const data = await response.json();
            if (data.success && data.data?.status === "AUTHORIZED") {
              const captureResponse = await fetch(
                `${API_ROOT}/payments/${checkout.payment.id}/capture`,
                {
                  method: "POST",
                  headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${checkout.token}`,
                  },
                }
              );
              const captureData = await captureResponse.json();
              if (captureData.success && captureData.data?.status === "CAPTURED") {
                navigate("/success", {
                  replace: true,
                  state: { transaction: buildTransaction("CAPTURED") },
                });
                return;
              }
            }
            pollBackendForStatus();
          } catch {
            pollBackendForStatus();
          }
        }}
        onResendOtp={() => {}}
        onCancel={handleCancel}
      />
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <motion.section
        initial={{ opacity: 0, scale: 0.97, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        className="w-full max-w-2xl overflow-hidden rounded-[2rem] border border-white/10 bg-slate-950 text-white shadow-[0_30px_120px_rgba(2,6,23,0.48)]"
      >
        <div className="grid gap-8 p-8 md:grid-cols-[0.9fr_1.1fr] md:p-10">
          <div className={`rounded-[1.75rem] border p-6 ${
            status === "redirecting_bank"
              ? "border-indigo-400/30 bg-[linear-gradient(180deg,rgba(99,102,241,0.3),rgba(15,23,42,0.92))]"
              : status === "pending_wallet"
              ? "border-purple-400/30 bg-[linear-gradient(180deg,rgba(168,85,247,0.3),rgba(15,23,42,0.92))]"
              : status === "failed"
              ? "border-red-400/30 bg-[linear-gradient(180deg,rgba(239,68,68,0.3),rgba(15,23,42,0.92))]"
              : "border-cyan-400/15 bg-[linear-gradient(180deg,rgba(14,116,144,0.3),rgba(15,23,42,0.92))]"
          }`}>
            {status === "failed" ? (
              <>
                <div className="mx-auto h-16 w-16 rounded-full border-4 border-red-400" />
                <h1 className="mt-6 text-2xl font-semibold tracking-tight text-red-400">
                  Payment Failed
                </h1>
                <p className="mt-3 text-sm leading-6 text-slate-300">
                  {error || "Redirecting..."}
                </p>
              </>
            ) : status === "redirecting_bank" ? (
              <>
                <div className="mx-auto h-14 w-14 rounded-xl bg-white flex items-center justify-center">
                  <svg width="32" height="32" viewBox="0 0 40 40">
                    <rect width="40" height="40" rx="6" fill="#6366f1" />
                    <rect y="28" width="40" height="5" fill="rgba(255,255,255,0.2)" rx="2" />
                    <rect x="12" y="8" width="16" height="16" rx="2" fill="rgba(255,255,255,0.3)" />
                    <text x="20" y="21" textAnchor="middle" fill="white" fontSize="12" fontWeight="bold">{checkout?.bankName?.charAt(0) || "B"}</text>
                  </svg>
                </div>
                <h1 className="mt-6 text-xl font-semibold tracking-tight text-indigo-300">
                  {checkout?.bankName || "Bank"} Portal
                </h1>
                <div className="mt-4 space-y-2">
                  <div className="flex items-center gap-2 text-sm text-slate-300">
                    <svg className="h-4 w-4 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" /></svg>
                    <span>Secure connection established</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-slate-300">
                    <svg className="h-4 w-4 text-indigo-400 animate-spin" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                    <span>Awaiting authentication...</span>
                  </div>
                </div>
                <p className="mt-4 text-xs leading-5 text-slate-500">
                  You will be redirected to {checkout?.bankName || "your bank"}'s secure login page
                </p>
              </>
            ) : status === "pending_wallet" ? (
              <>
                <div className="mx-auto h-14 w-14 rounded-xl bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center text-white text-xs font-bold shadow-lg">
                  {checkout?.walletName?.split(" ").map(w => w[0]).join("").slice(0, 2) || "W"}
                </div>
                <h1 className="mt-6 text-xl font-semibold tracking-tight text-purple-300">
                  {checkout?.walletName || "Wallet"}
                </h1>
                <div className="mt-4 space-y-2">
                  <div className="flex items-center gap-2 text-sm text-slate-300">
                    <svg className="h-4 w-4 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" /></svg>
                    <span>Wallet connected</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-slate-300">
                    <svg className="h-4 w-4 text-purple-400 animate-spin" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                    <span>Verifying PIN...</span>
                  </div>
                </div>
                <p className="mt-4 text-xs leading-5 text-slate-500">
                  Processing payment via {checkout?.walletName || "wallet"}
                </p>
              </>
            ) : (
              <>
                <div className="mx-auto h-16 w-16 animate-spin rounded-full border-4 border-cyan-300 border-t-transparent" />
                <h1 className="mt-6 text-2xl font-semibold tracking-tight">
                  Processing payment
                </h1>
                <p className="mt-3 text-sm leading-6 text-slate-300">
                  {pollAttempt > 0 
                    ? `Verifying payment (attempt ${pollAttempt}/10)`
                    : progressMessage}
                </p>
              </>
            )}
          </div>

          <div className="space-y-4">
            <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.24em] text-slate-400">
                Order reference
              </p>
              <p className="mt-2 text-lg font-semibold text-white">
                {checkout?.order?.externalReference ??
                  checkout?.order?.id ??
                  "Pending"}
              </p>
            </div>
            <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.24em] text-slate-400">
                Amount
              </p>
              <p className="mt-2 text-lg font-semibold text-white">
                {formatCurrency(checkout?.amount ?? 0)}
              </p>
            </div>
            <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.24em] text-slate-400">
                Correlation ID
              </p>
              <p className="mt-2 break-all text-sm font-medium text-cyan-200">
                {checkout?.correlationId ?? "Assigned by backend"}
              </p>
            </div>
          </div>
        </div>
      </motion.section>
    </main>
  );
}
