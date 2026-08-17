import { Badge } from "@/components/ui/badge";

type TripStatus = "DRAFT" | "PUBLISHED" | "CLOSED" | "EXPIRED";

const VARIANT: Record<TripStatus, "default" | "secondary" | "destructive" | "outline"> = {
  PUBLISHED: "default",
  DRAFT: "secondary",
  CLOSED: "outline",
  EXPIRED: "destructive",
};

export function TripStatusBadge({ status }: { status: string | undefined }) {
  const key = (status ?? "DRAFT") as TripStatus;
  return <Badge variant={VARIANT[key] ?? "outline"}>{key.charAt(0) + key.slice(1).toLowerCase()}</Badge>;
}
