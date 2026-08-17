import { Badge } from "@/components/ui/badge";

type CompanyStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";

const VARIANT: Record<CompanyStatus, "default" | "secondary" | "destructive" | "outline"> = {
  APPROVED: "default",
  PENDING: "secondary",
  REJECTED: "destructive",
  SUSPENDED: "destructive",
};

const LABEL: Record<CompanyStatus, string> = {
  APPROVED: "Approved",
  PENDING: "Pending review",
  REJECTED: "Rejected",
  SUSPENDED: "Suspended",
};

export function CompanyStatusBadge({ status }: { status: string | undefined }) {
  const key = (status ?? "PENDING") as CompanyStatus;
  return <Badge variant={VARIANT[key] ?? "outline"}>{LABEL[key] ?? status}</Badge>;
}
