import { BrowserRouter, Routes, Route } from "react-router-dom";
import Checkout from "./pages/Checkout";
import Success from "./pages/Success";
import Receipt from "./pages/Receipt";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Checkout />} />

        <Route path="/success" element={<Success />} />

        <Route path="/receipt" element={<Receipt />} />
      </Routes>
    </BrowserRouter>
  );
}
