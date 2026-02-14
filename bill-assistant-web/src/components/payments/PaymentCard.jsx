import { useState } from "react";
import Money from "../common/Money";
import PaymentTimeline from "./PaymentTimeline";
import PaymentModal from "./PaymentModal";

import { motion, AnimatePresence } from "framer-motion";

const statusBadge = {
  SUCCESS: "bg-green-100 text-green-700",
  PROCESSING: "bg-blue-100 text-blue-700",
  FAILED: "bg-red-100 text-red-700",
  REJECTED: "bg-red-100 text-red-700",
  CANCELLED: "bg-slate-200 text-slate-600",
};

export default function PaymentCard({ payment }) {
  const [open, setOpen] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  const badgeStyle =
    statusBadge[payment.status] ||
    "bg-slate-100 text-slate-600";

  return (
    <>
      <div className="bg-white rounded-2xl shadow overflow-hidden">
        {/* Main Card */}
        <div
          onClick={() => setOpen(!open)}
          className="p-5 cursor-pointer hover:bg-slate-50 transition"
        >
          <div className="flex justify-between items-start mb-2">
            <h3 className="font-semibold">
              {payment.providerName || "Payment"}
            </h3>

            <span
              className={`text-xs px-2 py-0.5 rounded-full font-medium ${badgeStyle}`}
            >
              {payment.status}
            </span>
          </div>

          <Money
            value={{
              amount: payment.amount,
              currency: payment.currency,
            }}
            className="text-lg font-semibold"
          />

          <p className="text-sm text-slate-500 mt-1">
            Type: {payment.paymentType}
          </p>

          <p className="text-sm text-slate-500 mt-1">
              Date: {payment.scheduledDate}
          </p>

          <p className="text-sm text-slate-500 mt-1">
              Txn Ref: {payment.gatewayReference}
          </p>

          <p className="text-sm text-slate-500 mt-1">
              Executed At: {payment.executedAt}
          </p>

          <p className="text-xs text-slate-400 mt-2">
            {open ? "▲ Hide details" : "▼ View details"}
          </p>
        </div>

        {/* Drawer */}
        <AnimatePresence>
          {open && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.25 }}
              className="border-t px-5 py-4 bg-slate-50 space-y-4"
            >
              <PaymentTimeline status={payment.status} />

              <div className="text-sm text-slate-600 space-y-1">
                <div>
                  <b>Method:</b> {payment.method || "N/A"}
                </div>
                <div>
                  <b>Created Date:</b>{" "}
                  {payment.createdAt
                    ? new Date(payment.createdAt).toLocaleString("en-IN")
                    : "N/A"}
                </div>
              </div>

              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setModalOpen(true);
                }}
                className="w-full bg-slate-900 text-white py-2.5 rounded-xl"
              >
                View Full Details →
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Modal */}
      {modalOpen && (
        <PaymentModal
          payment={payment}
          onClose={() => setModalOpen(false)}
        />
      )}
    </>
  );
}
