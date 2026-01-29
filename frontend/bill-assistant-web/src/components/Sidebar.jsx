import { Link } from "react-router-dom"

export default function Sidebar() {
  return (
    <div className="w-64 bg-gray-900 text-white p-4 hidden md:block">
      <h2 className="text-xl font-bold mb-6">Bill Assistant</h2>

      <nav className="space-y-4">
        <Link to="/bills" className="block hover:text-blue-400">Bills</Link>
        <Link to="/payments" className="block hover:text-blue-400">Payments</Link>
        <Link to="/chat" className="block hover:text-blue-400">Chat 🤖</Link>
      </nav>
    </div>
  )
}
