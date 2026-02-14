function Step({ label, active, completed, failed }) {
  const dotColor = failed
    ? "bg-red-500"
    : completed
    ? "bg-green-500"
    : active
    ? "bg-blue-500"
    : "bg-slate-300";

  const textColor = failed
    ? "text-red-600"
    : completed
    ? "text-green-600"
    : active
    ? "text-slate-900"
    : "text-slate-400";

  return (
    <div className="flex flex-col items-center gap-1 min-w-[60px]">
      {/* Dot */}
      <span
        className={`h-3 w-3 rounded-full ${dotColor} ${
          active ? "animate-pulse" : ""
        }`}
      />

      {/* Label */}
      <span className={`text-[11px] font-semibold ${textColor}`}>
        {label}
      </span>
    </div>
  );
}

function resolvePhase(status) {
  switch (status) {
    case "CREATED":
    case "SCHEDULED":
      return "CREATED";

    case "APPROVAL_PENDING":
    case "APPROVED":
      return "APPROVAL";

    case "PROCESSING":
      return "PROCESSING";

    case "SUCCESS":
      return "SUCCESS";

    case "FAILED":
    case "REJECTED":
    case "CANCELLED":
      return "FAILED";

    default:
      return "CREATED";
  }
}

export default function PaymentTimeline({ status }) {
  const phase = resolvePhase(status);

  // Connector fill helper
  const connector = (done) =>
    done ? "bg-green-400" : "bg-slate-300";

  return (
    <div className="flex items-center justify-between mt-4">
      {/* Created */}
      <Step
        label="Created"
        completed={phase !== "CREATED"}
        active={phase === "CREATED"}
      />

      <div className={`flex-1 h-[2px] mx-1 ${connector(phase !== "CREATED")}`} />

      {/* Approval */}
      <Step
        label="Approval"
        completed={["PROCESSING", "SUCCESS", "FAILED"].includes(phase)}
        active={phase === "APPROVAL"}
      />

      <div
        className={`flex-1 h-[2px] mx-1 ${connector(
          ["PROCESSING", "SUCCESS", "FAILED"].includes(phase)
        )}`}
      />

      {/* Processing */}
      <Step
        label="Processing"
        completed={["SUCCESS", "FAILED"].includes(phase)}
        active={phase === "PROCESSING"}
      />

      <div
        className={`flex-1 h-[2px] mx-1 ${connector(
          ["SUCCESS", "FAILED"].includes(phase)
        )}`}
      />

      {/* Result */}
      <Step
        label={phase === "FAILED" ? "Failed" : "Success"}
        completed={phase === "SUCCESS"}
        failed={phase === "FAILED"}
        active={phase === "PROCESSING"}
      />
    </div>
  );
}
