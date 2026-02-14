import { useEffect, useState, useRef } from "react";
import { getPayments } from "../api/paymentsApi";

import PaymentCard from "../components/payments/PaymentCard";
import PaymentsSkeleton from "../components/payments/PaymentsSkeleton";

import { motion } from "framer-motion";

const FILTERS = ["ALL", "SUCCESS", "FAILED", "PROCESSING"];
const PAGE_SIZE = 6;

export default function Payments() {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);

  const [filter, setFilter] = useState("ALL");
  const [search, setSearch] = useState("");

  // Infinite scroll visible count
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);

  // Observer trigger element
  const loadMoreRef = useRef(null);

  // ✅ Fetch payments
  useEffect(() => {
    async function loadPayments() {
      try {
        const data = await getPayments();
        setPayments(data || []);
      } catch (err) {
        console.error("Failed to load payments:", err);
      } finally {
        setLoading(false);
      }
    }

    loadPayments();
  }, []);

  // ✅ Filter + Search logic
  const filteredPayments = payments.filter((p) => {
    const matchesStatus =
      filter === "ALL" || p.status === filter;

    const matchesSearch =
      p.providerName?.toLowerCase().includes(search.toLowerCase()) ||
      p.referenceId?.toLowerCase().includes(search.toLowerCase());

    return matchesStatus && matchesSearch;
  });

  // Visible slice
  const visiblePayments = filteredPayments.slice(0, visibleCount);

  // ✅ Reset pagination when filter/search changes
  useEffect(() => {
    setVisibleCount(PAGE_SIZE);
  }, [filter, search]);

  // ✅ Infinite scroll observer
  useEffect(() => {
    if (!loadMoreRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (
          entries[0].isIntersecting &&
          visibleCount < filteredPayments.length
        ) {
          setVisibleCount((prev) => prev + PAGE_SIZE);
        }
      },
      { threshold: 1 }
    );

    observer.observe(loadMoreRef.current);

    return () => observer.disconnect();
  }, [visibleCount, filteredPayments.length]);

  // ✅ Status counts
  const counts = FILTERS.reduce((acc, f) => {
    acc[f] =
      f === "ALL"
        ? payments.length
        : payments.filter((p) => p.status === f).length;
    return acc;
  }, {});

  return (
    <div>
      {/* Title */}
      <h2 className="text-2xl font-bold mb-6">
        Payments
      </h2>

      {/* ✅ Search */}
      <input
        type="text"
        placeholder="Search by provider or transaction ID..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="w-full mb-5 px-4 py-2 border rounded-xl text-sm
          focus:ring-2 focus:ring-blue-500 outline-none"
      />

      {/* ✅ Filters with counts */}
      <div className="flex gap-2 mb-6 flex-wrap">
        {FILTERS.map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition ${
              filter === f
                ? "bg-blue-600 text-white"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            }`}
          >
            {f} ({counts[f]})
          </button>
        ))}
      </div>

      {/* ✅ Skeleton Loader */}
      {loading && <PaymentsSkeleton />}

      {/* ✅ Empty State */}
      {!loading && filteredPayments.length === 0 && (
        <p className="text-slate-500 text-sm">
          No payments found.
        </p>
      )}

      {/* ✅ Payments List */}
      <div className="space-y-4">
        {visiblePayments.map((payment, index) => (
          <motion.div
            key={payment.id || payment.referenceId || index} // ✅ FIXED KEY
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{
              duration: 0.3,
              delay: index * 0.05,
            }}
          >
            <PaymentCard payment={payment} />
          </motion.div>
        ))}
      </div>

      {/* ✅ Infinite Scroll Spinner */}
      {visibleCount < filteredPayments.length && (
        <div
          ref={loadMoreRef}
          className="flex justify-center py-8"
        >
          <div className="h-6 w-6 border-2 border-slate-300 border-t-slate-800 rounded-full animate-spin" />
        </div>
      )}
    </div>
  );
}
