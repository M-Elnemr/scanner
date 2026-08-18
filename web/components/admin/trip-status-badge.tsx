"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";

type TripStatus = "DRAFT" | "PUBLISHED" | "CLOSED" | "EXPIRED";

const VARIANT: Record<TripStatus, "default" | "secondary" | "destructive" | "outline"> = {
  PUBLISHED: "default",
  DRAFT: "secondary",
  CLOSED: "outline",
  EXPIRED: "destructive",
};

export function TripStatusBadge({ status }: { status: string | undefined }) {
  const t = useTranslations("admin.tripStatus");
  const key = (status ?? "DRAFT") as TripStatus;
  return <Badge variant={VARIANT[key] ?? "outline"}>{status && status in VARIANT ? t(key) : status}</Badge>;
}
