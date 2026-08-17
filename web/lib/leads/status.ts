/**
 * The backend's lead lifecycle is nine states deep (report/confirm handshakes for deposit, full
 * payment and commission) — internal workflow between the customer, the company and the platform.
 * Per product decision, the customer-facing site collapses all of that to three states and never
 * shows the raw LeadStatus enum or its history. Everything else in this file exists to make that
 * collapse the ONE place it happens, so no screen accidentally leaks a granular status string.
 */
export type SimpleLeadState = "preserved" | "completed" | "cancelled";

export function simplifyLeadStatus(status: string | undefined): SimpleLeadState {
  if (status === "CANCELLED") return "cancelled";
  if (status === "CASHBACK_PAID") return "completed";
  return "preserved";
}

export const SIMPLE_STATE_LABEL: Record<SimpleLeadState, string> = {
  preserved: "Preserved",
  completed: "Completed",
  cancelled: "Cancelled",
};
