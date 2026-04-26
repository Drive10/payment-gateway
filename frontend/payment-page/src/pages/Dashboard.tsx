import { useState, useEffect, useCallback } from "react";
import { useNavigate, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { getStoredAuth, logout, getOrderHistory, getPaymentHistory, formatCurrency } from "../lib/payment";

export default function Dashboard() {
  const navigate = useNavigate();
  const [auth] = useState(getStoredAuth());
  const [orders, setOrders] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("orders");
  const [error, setError] = useState("");

  const loadData = useCallback(async () => {
    if (!auth?.token) return;
    setLoading(true);
    setError("");
    try {
      const [ordersData, paymentsData] = await Promise.all([
        getOrderHistory(auth.token, 20, 0),
        getPaymentHistory(auth.token, 20, 0)
      ]);
      setOrders(ordersData?.data || ordersData || []);
      setPayments(paymentsData?.data || paymentsData || []);
    } catch (err) {
      setError(err.message || "Failed to load data");
    } finally {
      setLoading(false);
    }
  }, [auth?.token]);

  useEffect(() => {
    if (!auth?.token) {
      navigate("/login");
      return;
    }
    loadData();
  }, [auth, navigate, loadData]);

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const statusColors = {
    CREATED: "bg-blue-100 text-blue-700",
    PAID: "bg-green-100 text-green-700",
    PENDING: "bg-yellow-100 text-yellow-700",
    FAILED: "bg-red-100 text-red-700",
    REFUNDED: "bg-purple-100 text-purple-700",
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-cyan-50">
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-600 to-teal-600">
              <svg className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            <h1 className="text-xl font-bold text-slate-900">PayFlow Dashboard</h1>
          </div>
          <div className="flex items-center gap-4">
            <Link to="/" className="text-sm text-cyan-600 hover:text-cyan-700 font-medium">
              New Payment
            </Link>
            <div className="flex items-center gap-2">
              <div className="h-8 w-8 rounded-full bg-cyan-100 flex items-center justify-center">
                <span className="text-sm font-medium text-cyan-700">
                  {auth?.user?.firstName?.[0] || auth?.user?.email?.[0]?.toUpperCase() || "U"}
                </span>
              </div>
              <button
                onClick={handleLogout}
                className="text-sm text-slate-500 hover:text-slate-700"
              >
                Sign out
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-8">
        {error && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700">
            {error}
          </div>
        )}

        <div className="grid gap-6 lg:grid-cols-3">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="lg:col-span-2"
          >
            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <div className="border-b border-slate-100">
                <div className="flex">
                  <button
                    onClick={() => setActiveTab("orders")}
                    className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
                      activeTab === "orders"
                        ? "border-cyan-500 text-cyan-600"
                        : "border-transparent text-slate-500 hover:text-slate-700"
                    }`}
                  >
                    Orders
                  </button>
                  <button
                    onClick={() => setActiveTab("payments")}
                    className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
                      activeTab === "payments"
                        ? "border-cyan-500 text-cyan-600"
                        : "border-transparent text-slate-500 hover:text-slate-700"
                    }`}
                  >
                    Payments
                  </button>
                </div>
              </div>

              <div className="p-6">
                {loading ? (
                  <div className="flex items-center justify-center py-12">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cyan-600"></div>
                  </div>
                ) : activeTab === "orders" ? (
                  orders.length === 0 ? (
                    <div className="text-center py-12">
                      <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                      </svg>
                      <p className="mt-2 text-slate-500">No orders yet</p>
                      <Link to="/" className="mt-4 inline-block text-sm text-cyan-600 hover:text-cyan-700">
                        Create your first order →
                      </Link>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {orders.map((order) => (
                        <div
                          key={order.id}
                          className="flex items-center justify-between p-4 rounded-xl bg-slate-50 border border-slate-100"
                        >
                          <div>
                            <p className="font-medium text-slate-900">
                              {order.externalReference || order.id}
                            </p>
                            <p className="text-sm text-slate-500">
                              {order.customerEmail || "No customer"}
                            </p>
                          </div>
                          <div className="text-right">
                            <p className="font-semibold text-slate-900">
                              {formatCurrency(order.amount)}
                            </p>
                            <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[order.status] || "bg-slate-100 text-slate-600"}`}>
                              {order.status || "UNKNOWN"}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )
                ) : payments.length === 0 ? (
                  <div className="text-center py-12">
                    <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
                    </svg>
                    <p className="mt-2 text-slate-500">No payments yet</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {payments.map((payment) => (
                      <div
                        key={payment.id}
                        className="flex items-center justify-between p-4 rounded-xl bg-slate-50 border border-slate-100"
                      >
                        <div>
                          <p className="font-medium text-slate-900">
                            {payment.orderReference || payment.id}
                          </p>
                          <p className="text-sm text-slate-500">
                            {payment.method} • {payment.provider}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="font-semibold text-slate-900">
                            {formatCurrency(payment.amount)}
                          </p>
                          <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[payment.status] || "bg-slate-100 text-slate-600"}`}>
                            {payment.status || "UNKNOWN"}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
              <h3 className="font-semibold text-slate-900 mb-4">Account</h3>
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-cyan-100 flex items-center justify-center">
                    <span className="text-sm font-medium text-cyan-700">
                      {auth?.user?.firstName?.[0] || auth?.user?.email?.[0]?.toUpperCase() || "U"}
                    </span>
                  </div>
                  <div>
                    <p className="font-medium text-slate-900">
                      {auth?.user?.firstName} {auth?.user?.lastName}
                    </p>
                    <p className="text-sm text-slate-500">{auth?.user?.email}</p>
                  </div>
                </div>
              </div>

              <div className="mt-6 pt-6 border-t border-slate-100">
                <Link
                  to="/"
                  className="block w-full text-center py-2.5 px-4 rounded-xl border border-slate-200 text-slate-700 font-medium hover:border-slate-300 transition-colors"
                >
                  Create New Payment
                </Link>
              </div>
            </div>
          </motion.div>
        </div>
      </main>
    </div>
  );
}