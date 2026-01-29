import { Routes, Route, NavLink } from "react-router-dom";
import Bills from "./pages/Bills";
import Payments from "./pages/Payments";
import Chat from "./pages/Chat";
import UploadBills from "./pages/UploadBills";

const linkClass = ({ isActive }) =>
  isActive ? "text-blue-600 font-semibold" : "text-slate-500";

export default function App() {
  return (
    <div className="min-h-screen bg-slate-100 pb-16 sm:pb-0">
      {/* Desktop top nav */}
      <header className="bg-white shadow hidden sm:block">
        <div className="max-w-6xl mx-auto px-6 py-4 flex gap-6">
          <NavLink to="/bills" className={linkClass}>
            Bills
          </NavLink>
          <NavLink to="/payments" className={linkClass}>
            Payments
          </NavLink>
          <NavLink to="/upload" className={linkClass}>
            Upload
          </NavLink>
          <NavLink to="/chat" className={linkClass}>
            Chat
          </NavLink>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6">
        <Routes>
          <Route path="/" element={<Bills />} />
          <Route path="/bills" element={<Bills />} />
          <Route path="/payments" element={<Payments />} />
          <Route path="/upload" element={<UploadBills />} />
          <Route path="/chat" element={<Chat />} />
        </Routes>
      </main>

      {/* Mobile bottom nav */}
      <nav className="sm:hidden fixed bottom-0 inset-x-0 bg-white border-t flex justify-around py-3">
        <NavLink to="/bills" className={linkClass}>
          Bills
        </NavLink>
        <NavLink to="/payments" className={linkClass}>
          Payments
        </NavLink>
        <NavLink to="/upload" className={linkClass}>
          Upload
        </NavLink>
        <NavLink to="/chat" className={linkClass}>
          Chat
        </NavLink>
      </nav>
    </div>
  );
}
