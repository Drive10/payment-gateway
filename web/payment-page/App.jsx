import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Checkout from "./pages/Checkout";
import Processing from "./pages/Processing";
import Success from "./pages/Success";
import Failure from "./pages/Failure";
import Receipt from "./pages/Receipt";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import { getStoredAuth } from "./lib/payment";

function ProtectedRoute({ children }) {
  const auth = getStoredAuth();
  if (!auth?.token) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Checkout />} />
        <Route path="/login" element={<Login />} />
        <Route path="/processing" element={<Processing />} />
        <Route path="/success" element={<Success />} />
        <Route path="/failure" element={<Failure />} />
        <Route path="/receipt" element={<Receipt />} />
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } 
        />
      </Routes>
    </BrowserRouter>
  );
}