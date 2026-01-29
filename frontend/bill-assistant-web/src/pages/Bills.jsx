import { useEffect, useState } from "react";
import { getBills, payBill } from "../api/billsApi";
import { useNavigate } from "react-router-dom";

const statusColor = {
  PAID: "text-green-600",
  OVERDUE: "text-red-600",
  PENDING: "text-yellow-600",
};

export default function Bills() {
  const [bills, setBills] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const navigate = useNavigate();

  const loadBills = () => {
    getBills(page).then((p) => {
      setBills(p.content ?? []);
      setTotalPages(p.totalPages);
    });
  };

  useEffect(loadBills, [page]);

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Your Bills</h2>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {bills.map((b) => (
          <div
            key={b.id}
            className="bg-white rounded-xl shadow p-4 flex flex-col"
          >
            <div className="flex justify-between mb-2">
              <h3 className="font-semibold">{b.vendor}</h3>
              <span className={`text-sm font-medium ${statusColor[b.status]}`}>
                {b.status}
              </span>
            </div>

            <p className="text-slate-700 mb-1">₹{b.amount}</p>
            <p className="text-sm text-slate-500 mb-4">Due: {b.dueDate}</p>

            <div className="mt-auto flex gap-2">
              {b.status !== "PAID" && (
                <button
                  onClick={async () => {
                    await payBill(b.id);
                    loadBills();
                  }}
                  className="flex-1 bg-green-600 hover:bg-green-700 text-white text-sm py-2 rounded-lg"
                >
                  Pay
                </button>
              )}

              <button
                onClick={() =>
                  navigate("/chat", {
                    state: { message: `Help me with bill ${b.id}` },
                  })
                }
                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white text-sm py-2 rounded-lg"
              >
                Ask Bot
              </button>
            </div>
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
