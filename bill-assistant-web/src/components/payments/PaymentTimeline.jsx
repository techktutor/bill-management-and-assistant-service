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
    ? "text-slate-800"
    : "text-slate-400";

  return (
    <div className="flex items-center gap-2">
      <span className={`h-2.5 w-2.5 rounded-full ${dotColor}`} />
      <span className={`text-xs font-medium ${textColor}`}>
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

  return (
    <div className="flex items-center gap-4 mt-4">
      {/* Created */}
      <Step
        label="Created"
        completed={phase !== "CREATED"}
        active={phase === "CREATED"}
      />

      <div className="flex-1 h-px bg-slate-300" />

      {/* Approval */}
      <Step
        label="Approval"
        completed={["PROCESSING", "SUCCESS", "FAILED"].includes(phase)}
        active={phase === "APPROVAL"}
      />

      <div className="flex-1 h-px bg-slate-300" />

      {/* Processing */}
      <Step
        label="Processing"
        completed={["SUCCESS", "FAILED"].includes(phase)}
        active={phase === "PROCESSING"}
      />

      <div className="flex-1 h-px bg-slate-300" />

      {/* Result */}
      <Step
        label={
          phase === "FAILED"
            ? "Failed"
            : phase === "SUCCESS"
            ? "Success"
            : "Result"
        }
        completed={phase === "SUCCESS"}
        failed={phase === "FAILED"}
        active={phase === "PROCESSING"}
      />
    </div>
  );
}
