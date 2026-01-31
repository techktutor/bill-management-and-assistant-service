import { differenceInCalendarDays, parseISO } from "date-fns";

function getBadge(days) {
  if (days < 0) {
    return { text: "Overdue", color: "bg-red-100 text-red-700" };
  }
  if (days === 0) {
    return { text: "Due today", color: "bg-red-100 text-red-700" };
  }
  if (days <= 3) {
    return {
      text: `${days} day${days > 1 ? "s" : ""} left`,
      color: "bg-orange-100 text-orange-700",
    };
  }
  if (days <= 7) {
    return {
      text: `${days} days left`,
      color: "bg-yellow-100 text-yellow-700",
    };
  }
  return {
    text: `Due in ${days} days`,
    color: "bg-slate-100 text-slate-600",
  };
}

export default function DueDateBadge({ dueDate, status }) {
  if (!dueDate || status === "PAID") return null;

  const days = differenceInCalendarDays(
    parseISO(dueDate),
    new Date()
  );

  const badge = getBadge(days);

  return (
    <span
      className={`inline-flex items-center gap-1 text-xs font-medium
        px-2 py-0.5 rounded-full ${badge.color}`}
    >
      <span aria-hidden>⏰</span>
      {badge.text}
    </span>
  );
}
