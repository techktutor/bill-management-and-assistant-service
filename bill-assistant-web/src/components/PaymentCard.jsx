import Money from "../common/Money";
import PaymentTimeline from "./PaymentTimeline";
import { useNavigate } from "react-router-dom";

const statusBadge = {
  CREATED: "bg-slate-100 text-slate-600",
  SCHEDULED: "bg-slate-100 text-slate-600",

  APPROVAL_PENDING: "bg-yellow-100 text-yellow-700",
  APPROVED: "bg-green-100 text-green-700",

  PROCESSING: "bg-blue-100 text-blue-700",

  SUCCESS: "bg-green-100 text-green-700",

  FAILED: "bg-red-100 text-red-700",
  REJECTED: "bg-red-100 text-red-700",
  CANCELLED: "bg-slate-200 text-slate-600",
};

export default function PaymentCard({ payment }) {
  const navigate = useNavigate();

  return (
    <div className="relative bg-white rounded-xl shadow p-5">
      {/* Header */}
      <div className="flex justify-between items-start mb-2">
        <h3 className="font-semibold">
          {payment.providerName || "Payment"}
        </h3>

        <span
          className={`text-xs px-2 py-0.5 rounded-full font-medium ${
            statusBadge[payment.status]
          }`}
        >
          {payment.status}
        </span>
      </div>

      {/* Amount */}
      <Money
        value={payment.amount}
        className="text-lg font-semibold"
      />

      {/* Meta */}
      <div className="text-sm text-slate-500 mt-2 space-y-1">
        {payment.createdAt && (
          <div>
            Initiated on{" "}
            {new Date(payment.createdAt).toLocaleDateString("en-IN")}
          </div>
        )}

        {payment.referenceId && (
          <div>Txn Ref: {payment.referenceId}</div>
        )}
      </div>

      {/* Timeline */}
      <PaymentTimeline status={payment.status} />

      {/* Action for failure */}
      {(payment.status === "FAILED" ||
        payment.status === "REJECTED") && (
        <button
          onClick={() =>
            navigate("/chat", {
              state: {
                message: `Why did payment ${payment.referenceId} fail?`,
              },
            })
          }
          className="mt-4 w-full bg-blue-600 hover:bg-blue-700
            text-white text-sm py-2 rounded-lg"
        >
          Ask Bot 🤖
        </button>
      )}
    </div>
  );
}
