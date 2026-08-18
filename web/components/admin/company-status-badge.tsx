"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";

type CompanyStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";

const VARIANT: Record<CompanyStatus, "default" | "secondary" | "destructive" | "outline"> = {
  APPROVED: "default",
  PENDING: "secondary",
  REJECTED: "destructive",
  SUSPENDED: "destructive",
};

export function CompanyStatusBadge({ status }: { status: string | undefined }) {
  const t = useTranslations("admin.companyStatus");
  const key = (status ?? "PENDING") as CompanyStatus;
  return <Badge variant={VARIANT[key] ?? "outline"}>{status && status in VARIANT ? t(key) : status}</Badge>;
}
