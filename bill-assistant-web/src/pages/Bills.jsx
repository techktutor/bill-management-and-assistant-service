import { useEffect, useState } from "react";
import { getBills } from "../api/billsApi";
import { useNavigate } from "react-router-dom";
import Money from "../components/common/Money";
import DueDateBadge from "../components/common/DueDateBadge";

const confidenceColor = {
  HIGH_CONFIDENCE: "text-green-600",
  MEDIUM_CONFIDENCE: "text-yellow-600",
  LOW_CONFIDENCE: "text-red-600",
};

const confidenceLabel = {
  HIGH_CONFIDENCE: "HIGH",
  MEDIUM_CONFIDENCE: "MEDIUM",
  LOW_CONFIDENCE: "LOW",
};

const billStatusColor = {
  PAID: "text-green-600",
  UNPAID: "text-red-600",
  PENDING: "text-yellow-600",
};

export default function Bills() {
  const [bills, setBills] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    getBills(page).then((p) => {
      setBills(p.content ?? []);
      setTotalPages(p.totalPages ?? 0);
    });
  }, [page]);

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Your Bills</h2>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {bills.map((b) => (
          <div
            key={b.id}
            className="relative bg-white rounded-xl shadow p-4 flex flex-col"
          >
            {/* Provider + Status */}
            <div className="flex justify-between mb-2">
              <h3 className="font-semibold">
                {b.providerName || "Unknown Provider"}
              </h3>

              <span
                className={`text-xs font-medium ${
                  billStatusColor[b.status]
                }`}
              >
                {b.status}
              </span>
            </div>

            {/* Amount */}
            <Money
              value={b.amountDue}
              className="text-lg font-semibold"
            />

            {/* Due date */}
            <div className="flex items-center justify-between mt-1">
              <p className="text-sm text-slate-500">
                Due: {b.dueDate}
              </p>

              <DueDateBadge
                dueDate={b.dueDate}
                status={b.status}
              />
            </div>

            {/* Ask Bot (UNCHANGED) */}
            <div className="mt-auto pt-4">
              <button
                onClick={() =>
                  navigate("/chat", {
                    state: { message: `Help me with bill ${b.id}` },
                  })
                }
                className="w-full bg-blue-600 hover:bg-blue-700 text-white text-sm py-2 rounded-lg"
              >
                Ask Bot
              </button>
            </div>

            {/* Confidence badge (bottom-right) */}
            <span
              className={`group absolute bottom-2 right-2 text-xs font-semibold
                px-2 py-0.5 rounded-full bg-slate-100 cursor-help
                ${confidenceColor[b.confidenceDecision]}
                animate-fadeIn
              `}
            >
              {confidenceLabel[b.confidenceDecision]}

              {/* Tooltip */}
              <span
                className="absolute bottom-full right-0 mb-1 w-max max-w-[160px]
                  rounded bg-slate-900 text-white text-[10px]
                  px-2 py-1 opacity-0 group-hover:opacity-100
                  transition-opacity duration-200
                  pointer-events-none
                "
              >
                Data confidence based on bill text extraction accuracy
              </span>
            </span>
          </div>
        ))}
      </div>

      {/* Pagination */}
      <div className="flex justify-center gap-4 mt-6">
        <button
          disabled={page === 0}
          onClick={() => setPage((p) => p - 1)}
          className="px-4 py-2 bg-white rounded shadow disabled:opacity-50"
        >
          Prev
        </button>

        <span className="text-sm text-slate-600 mt-2">
          Page {page + 1} of {totalPages}
        </span>

        <button
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
          className="px-4 py-2 bg-white rounded shadow disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </div>
  );
}
