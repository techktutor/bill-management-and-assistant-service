import { useEffect, useState } from "react";
import { getPayments } from "../api/paymentsApi";
import { useNavigate } from "react-router-dom";
import Money from "../components/common/Money";

const statusColor = {
  SUCCESS: "text-green-600 bg-green-100",
  PROCESSING: "text-yellow-700 bg-yellow-100",
  FAILED: "text-red-600 bg-red-100",
};

const confidenceColor = {
  HIGH_CONFIDENCE: "text-green-600",
  MEDIUM_CONFIDENCE: "text-yellow-600",
  LOW_CONFIDENCE: "text-red-600",
};

export default function Payments() {
  const [payments, setPayments] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    getPayments().then(setPayments);
  }, []);

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Payments</h2>

      <div className="space-y-4">
        {payments.map((p) => (
          <div
            key={p.id}
            className="relative bg-white rounded-xl shadow p-4"
          >
            {/* Header */}
            <div className="flex justify-between items-start">
              <h3 className="font-semibold">
                {p.providerName || "Unknown Provider"}
              </h3>

              <span
                className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                  statusColor[p.status]
                }`}
              >
                {p.status}
              </span>
            </div>

            {/* Amount */}
            <Money
              value={{ amount: p.amount, currency: p.currency }}
              className="text-lg font-semibold mt-1"
            />

            {/* Meta */}
            <div className="text-sm text-slate-500 mt-2 space-y-1">
              <div>Paid on: {p.paidAt}</div>
              <div>Method: {p.method}</div>
              <div>Txn Ref: {p.referenceId}</div>
            </div>

            {/* Actions */}
            <div className="flex gap-2 mt-4">
              <button
                onClick={() => navigate(`/bills/${p.billId}`)}
                className="flex-1 bg-slate-100 hover:bg-slate-200 text-sm py-2 rounded-lg"
              >
                View Bill
              </button>

              <button
                onClick={() =>
                  navigate("/chat", {
                    state: {
                      message: `Explain payment ${p.referenceId}`,
                    },
                  })
                }
                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white text-sm py-2 rounded-lg"
              >
                Ask Bot
              </button>
            </div>

            {/* Confidence (bottom-right, subtle) */}
            <span
              className={`absolute bottom-2 right-2 text-xs ${
                confidenceColor[p.confidenceDecision]
              }`}
            >
              {p.confidenceDecision?.replace("_CONFIDENCE", "")}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
