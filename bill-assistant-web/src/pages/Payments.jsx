import { useEffect, useState } from "react";
import { getPayments } from "../api/paymentsApi";

export default function Payments() {
  const [payments, setPayments] = useState([]);

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
            className="bg-white rounded-xl shadow p-4 flex justify-between"
          >
            <div>
              <p className="font-medium">₹{p.amount}</p>
              <p className="text-sm text-slate-500">{p.createdAt}</p>
            </div>
            <span className="text-sm font-semibold">{p.status}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
