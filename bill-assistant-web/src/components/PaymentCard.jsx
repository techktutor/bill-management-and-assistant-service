export default function PaymentCard({ payment }) {
  const statusColor =
    payment.status === "SUCCESS"
      ? "text-green-600"
      : payment.status === "FAILED"
      ? "text-red-600"
      : "text-yellow-600"

  return (
    <div className="bg-white rounded-xl shadow p-5">
      <div className="flex justify-between items-center mb-2">
        <h3 className="font-semibold">Payment #{payment.id}</h3>
        <span className={`text-sm font-medium ${statusColor}`}>
          {payment.status}
        </span>
      </div>

      <p className="text-gray-700">Amount: ₹{payment.amount}</p>
      <p className="text-gray-500 text-sm">
        Date: {payment.createdAt}
      </p>

      {payment.status === "FAILED" && (
        <p className="text-sm text-red-500 mt-2">
          Retry via chatbot 🤖
        </p>
      )}
    </div>
  )
}
