"use client";

import { Check } from "lucide-react";
import { useTranslations } from "next-intl";
import { useCompare } from "@/components/trip/compare-store";
import { cn } from "@/lib/utils";

export function CompareToggle({ tripId }: { tripId: string }) {
  const { isSelected, toggle, ids, max } = useCompare();
  const t = useTranslations("trips");
  const selected = isSelected(tripId);
  const disabled = !selected && ids.length >= max;

  return (
    <button
      type="button"
      title={disabled ? t("compareUpToMax", { max }) : selected ? t("removeFromCompare") : t("addToCompare")}
      disabled={disabled}
      onClick={(e) => {
        e.preventDefault();
        e.stopPropagation();
        toggle(tripId);
      }}
      className={cn(
        "absolute top-3 right-3 flex size-6 items-center justify-center rounded-md border-2 bg-white/90 shadow-sm transition-colors",
        selected ? "border-primary bg-primary text-primary-foreground" : "border-white/70 text-transparent",
        disabled && "cursor-not-allowed opacity-50",
      )}
    >
      <Check className="size-3.5" strokeWidth={3} />
    </button>
  );
}
