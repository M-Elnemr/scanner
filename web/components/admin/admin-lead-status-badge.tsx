import { Badge } from "@/components/ui/badge";

type LeadStatus =
  | "INTERESTED"
  | "PENDING_DEPOSIT_CONFIRMATION"
  | "DEPOSIT_PAID"
  | "PENDING_FULL_PAYMENT_CONFIRMATION"
  | "FULLY_PAID"
  | "PENDING_COMMISSION_CONFIRMATION"
  | "COMMISSION_PAID"
  | "CASHBACK_PAID"
  | "CANCELLED";

const LABEL: Record<LeadStatus, string> = {
  INTERESTED: "Interested",
  PENDING_DEPOSIT_CONFIRMATION: "Deposit pending confirmation",
  DEPOSIT_PAID: "Deposit paid",
  PENDING_FULL_PAYMENT_CONFIRMATION: "Full payment pending confirmation",
  FULLY_PAID: "Fully paid",
  PENDING_COMMISSION_CONFIRMATION: "Commission pending confirmation",
  COMMISSION_PAID: "Commission paid",
  CASHBACK_PAID: "Cashback paid",
  CANCELLED: "Cancelled",
};

const VARIANT: Record<LeadStatus, "default" | "secondary" | "destructive" | "outline"> = {
  INTERESTED: "secondary",
  PENDING_DEPOSIT_CONFIRMATION: "secondary",
  DEPOSIT_PAID: "outline",
  PENDING_FULL_PAYMENT_CONFIRMATION: "secondary",
  FULLY_PAID: "outline",
  PENDING_COMMISSION_CONFIRMATION: "secondary",
  COMMISSION_PAID: "outline",
  CASHBACK_PAID: "default",
  CANCELLED: "destructive",
};

export function AdminLeadStatusBadge({ status }: { status: string | undefined }) {
  const key = (status ?? "INTERESTED") as LeadStatus;
  return <Badge variant={VARIANT[key] ?? "outline"}>{LABEL[key] ?? status}</Badge>;
}

export { LABEL as LEAD_STATUS_LABEL };
export type { LeadStatus };
