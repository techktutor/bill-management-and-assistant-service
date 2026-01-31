import { payBill } from "../api/billsApi"
import { useNavigate } from "react-router-dom"

export default function BillCard({ bill, onAction }) {
  const navigate = useNavigate()

  const statusColor =
    bill.status === "PAID"
      ? "text-green-600"
      : bill.status === "OVERDUE"
      ? "text-red-600"
      : "text-yellow-600"

  return (
    <div className="bg-white rounded-xl shadow p-5 flex flex-col gap-2">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold">{bill.vendor}</h3>
        <span className={`text-sm font-medium ${statusColor}`}>
          {bill.status}
        </span>
      </div>

      <p className="text-gray-700">Amount: ₹{bill.amount}</p>
      <p className="text-gray-500 text-sm">Due: {bill.dueDate}</p>

      <div className="flex gap-3 mt-4">
        {bill.status !== "PAID" && (
          <button
            onClick={async () => {
              await payBill(bill.id)
              onAction()
            }}
            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg text-sm"
          >
            Pay Now
          </button>
        )}

        <button
          onClick={() =>
            navigate("/chat", {
              state: { message: `Help me with bill ${bill.id}` }
            })
          }
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm"
        >
          Ask Bot 🤖
        </button>
      </div>
    </div>
  )
}
