import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { payBill } from "../api/billsApi";

import Money from "../components/common/Money";
import DueDateBadge from "../components/common/DueDateBadge";

/* -------------------------------
   Styling Maps (centralized)
-------------------------------- */

const STATUS_COLOR = {
  PAID: "text-green-600",
  UNPAID: "text-red-600",
  OVERDUE: "text-red-700",
  PENDING: "text-yellow-600",
};

const CONFIDENCE_COLOR = {
  HIGH_CONFIDENCE: "text-green-600",
  MEDIUM_CONFIDENCE: "text-yellow-600",
  LOW_CONFIDENCE: "text-red-600",
};

const CONFIDENCE_LABEL = {
  HIGH_CONFIDENCE: "HIGH",
  MEDIUM_CONFIDENCE: "MEDIUM",
  LOW_CONFIDENCE: "LOW",
};

export default function BillCard({ bill, onRefresh }) {
  const navigate = useNavigate();
  const [paying, setPaying] = useState(false);

  const status = bill.status ?? "UNPAID";
  const confidence = bill.confidenceDecision ?? "LOW_CONFIDENCE";

  /* -------------------------------
     Actions
  -------------------------------- */

  const handlePay = async () => {
    try {
      setPaying(true);
      await payBill(bill.id);
      onRefresh?.();
    } catch (err) {
      alert("❌ Payment failed. Please try again.");
    } finally {
      setPaying(false);
    }
  };

  const handleAskBot = () => {
    navigate("/chat", {
      state: {
        message: `Help me with my bill from ${bill.providerName} (₹${bill.amountDue?.amount}) due on ${bill.dueDate}`,
      },
    });
  };

  return (
    <div className="relative bg-white rounded-xl shadow p-5 flex flex-col gap-3">
      {/* Provider + Status */}
      <div className="flex justify-between items-start">
        <h3 className="text-lg font-semibold">
          {bill.providerName || "Unknown Provider"}
        </h3>

        <span className={`text-sm font-medium ${STATUS_COLOR[status]}`}>
          {status}
        </span>
      </div>

      {/* Amount */}
      <Money value={bill.amountDue} className="text-lg font-bold" />

      {/* Due Date */}
      <div className="flex justify-between items-center text-sm text-slate-500">
        <p>Due: {bill.dueDate}</p>
        <DueDateBadge dueDate={bill.dueDate} status={status} />
      </div>

      {/* Actions */}
      <div className="flex gap-3 mt-3">
        {status !== "PAID" && (
          <button
            disabled={paying}
            onClick={handlePay}
            className="flex-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white px-4 py-2 rounded-lg text-sm"
          >
            {paying ? "Processing..." : "Pay Now"}
          </button>
        )}

        <button
          onClick={handleAskBot}
          className="flex-1 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm"
        >
          Ask Eagle 🤖
        </button>
      </div>

      {/* Confidence Badge */}
      <span
        className={`group absolute bottom-2 right-2 text-xs font-semibold
        px-2 py-0.5 rounded-full bg-slate-100 cursor-help
        ${CONFIDENCE_COLOR[confidence]}
      `}
      >
        {CONFIDENCE_LABEL[confidence]}

        {/* Tooltip */}
        <span
          className="absolute bottom-full right-0 mb-1 w-max max-w-[160px]
          rounded bg-slate-900 text-white text-[10px]
          px-2 py-1 opacity-0 group-hover:opacity-100
          transition-opacity duration-200
          pointer-events-none"
        >
          Data confidence based on bill extraction accuracy
        </span>
      </span>
    </div>
  );
}
