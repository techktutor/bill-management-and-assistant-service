import { useEffect } from "react";
import Money from "../common/Money";
import { motion, AnimatePresence } from "framer-motion";

export default function PaymentModal({ payment, onClose }) {
  // ESC Close
  useEffect(() => {
    const handleEsc = (e) =>
      e.key === "Escape" && onClose();

    window.addEventListener("keydown", handleEsc);
    return () =>
      window.removeEventListener("keydown", handleEsc);
  }, [onClose]);

  return (
    <AnimatePresence>
      <motion.div
        className="fixed inset-0 z-50 flex justify-center items-end sm:items-center"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
      >
        {/* Background */}
        <div
          onClick={onClose}
          className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        />

        {/* Modal */}
        <motion.div
          initial={{ y: 200, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 200, opacity: 0 }}
          transition={{ duration: 0.35 }}
          className="relative bg-white w-full sm:max-w-lg rounded-t-3xl sm:rounded-3xl shadow-xl p-7"
        >
          <button
            onClick={onClose}
            className="absolute top-4 right-4 text-slate-500"
          >
            ✕
          </button>

          <h2 className="text-xl font-bold mb-4">
            Payment Details
          </h2>

          <Money
            value={{
              amount: payment.amount,
              currency: payment.currency,
            }}
            className="text-3xl font-semibold"
          />

          <div className="mt-6 space-y-3 text-sm text-slate-600">
            <Detail label="Status" value={payment.status} />
            <Detail label="Provider" value={payment.providerName} />
            <Detail label="Reference ID" value={payment.referenceId} />
            <Detail label="Method" value={payment.method || "N/A"} />
          </div>

          <button className="mt-8 w-full bg-blue-600 text-white py-3 rounded-xl">
            Ask Eagle 🤖
          </button>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}

function Detail({ label, value }) {
  return (
    <div className="flex justify-between border-b pb-2">
      <span className="font-medium">{label}</span>
      <span>{value}</span>
    </div>
  );
}
