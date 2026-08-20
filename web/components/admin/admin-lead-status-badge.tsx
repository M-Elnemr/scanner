"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";

type LeadStatus =
  | "INTERESTED"
  | "CONTACTED"
  | "PENDING_DEPOSIT_CONFIRMATION"
  | "DEPOSIT_PAID"
  | "PENDING_FULL_PAYMENT_CONFIRMATION"
  | "FULLY_PAID"
  | "PENDING_COMMISSION_CONFIRMATION"
  | "COMMISSION_PAID"
  | "CASHBACK_PAID"
  | "CANCELLED";

const ALL_LEAD_STATUSES: LeadStatus[] = [
  "INTERESTED",
  "CONTACTED",
  "PENDING_DEPOSIT_CONFIRMATION",
  "DEPOSIT_PAID",
  "PENDING_FULL_PAYMENT_CONFIRMATION",
  "FULLY_PAID",
  "PENDING_COMMISSION_CONFIRMATION",
  "COMMISSION_PAID",
  "CASHBACK_PAID",
  "CANCELLED",
];

const VARIANT: Record<LeadStatus, "default" | "secondary" | "destructive" | "outline"> = {
  INTERESTED: "secondary",
  CONTACTED: "secondary",
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
  const t = useTranslations("admin.leadStatus");
  const key = (status ?? "INTERESTED") as LeadStatus;
  return <Badge variant={VARIANT[key] ?? "outline"}>{status && status in VARIANT ? t(key) : status}</Badge>;
}

export function useLeadStatusLabel() {
  const t = useTranslations("admin.leadStatus");
  return (status: LeadStatus) => t(status);
}

export { ALL_LEAD_STATUSES };
export type { LeadStatus };
