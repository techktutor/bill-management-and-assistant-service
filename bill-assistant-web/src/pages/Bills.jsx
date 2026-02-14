import { useEffect, useState } from "react";
import { getBills } from "../api/billsApi";

import BillCard from "../components/BillCard";

/* -------------------------------
   Bills Page
-------------------------------- */

export default function Bills() {
  const [bills, setBills] = useState([]);

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  /* -------------------------------
     Fetch Bills
  -------------------------------- */

  const fetchBills = async () => {
    try {
      setLoading(true);
      setError(null);

      const res = await getBills(page);

      setBills(res.content ?? []);
      setTotalPages(res.totalPages ?? 0);
    } catch {
      setError("❌ Failed to load bills.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBills();
  }, [page]);

  /* -------------------------------
     UI States
  -------------------------------- */

  if (loading) {
    return <p className="text-slate-500">Loading bills...</p>;
  }

  if (error) {
    return (
      <div className="text-red-600 font-medium">
        {error}
        <button
          onClick={fetchBills}
          className="ml-4 px-3 py-1 bg-red-600 text-white rounded"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Your Bills</h2>

      {/* Empty State */}
      {bills.length === 0 ? (
        <p className="text-slate-500">🎉 No bills found.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {bills.map((bill) => (
            <BillCard key={bill.id} bill={bill} onRefresh={fetchBills} />
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-4 mt-8">
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
      )}
    </div>
  );
}
