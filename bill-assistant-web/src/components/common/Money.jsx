import { formatCurrency } from "../../utils/currency";

/**
 * Subtle color-by-amount logic
 * (only applies if parent doesn't specify a color)
 */
function getAmountColor(amount) {
  if (amount >= 10000) return "text-red-600";     // high
  if (amount >= 1000) return "text-yellow-600";   // medium
  return "text-slate-600";                        // low
}

export default function Money({ value, className = "" }) {
  if (!value || value.amount == null) return null;

  const hasExplicitColor = className.includes("text-");
  const amountColor = hasExplicitColor ? "" : getAmountColor(value.amount);

  return (
    <span className={`${amountColor} ${className}`}>
      {formatCurrency(value.amount, value.currency)}
    </span>
  );
}
