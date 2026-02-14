export default function PaymentsSkeleton() {
  return (
    <div className="space-y-4">
      {[...Array(5)].map((_, i) => (
        <div
          key={i}
          className="relative bg-white rounded-2xl shadow p-6 overflow-hidden"
        >
          {/* Shimmer */}
          <div className="absolute inset-0 -translate-x-full animate-shimmer
            bg-gradient-to-r from-transparent via-slate-200/60 to-transparent" />

          <div className="flex justify-between mb-5">
            <div className="h-4 w-36 bg-slate-200 rounded-md" />
            <div className="h-4 w-20 bg-slate-200 rounded-full" />
          </div>

          <div className="h-7 w-28 bg-slate-200 rounded-md mb-6" />

          <div className="space-y-3">
            <div className="h-3 w-52 bg-slate-200 rounded-md" />
            <div className="h-3 w-44 bg-slate-200 rounded-md" />
          </div>

          <div className="h-11 w-full bg-slate-200 rounded-xl mt-7" />
        </div>
      ))}
    </div>
  );
}
