"use client";

import { useRouter } from "next/navigation";
import { Search } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";

const QUICK_TIER_VALUES = [undefined, "VIP", "PREMIUM", "ECONOMIC"] as const;

export function HeroSearchCta() {
  const router = useRouter();
  const t = useTranslations("home");
  const tTiers = useTranslations("tiers");

  const tierLabel = (value: (typeof QUICK_TIER_VALUES)[number]) => {
    switch (value) {
      case "VIP":
        return tTiers("vip");
      case "PREMIUM":
        return tTiers("premium");
      case "ECONOMIC":
        return tTiers("economic");
      default:
        return tTiers("all");
    }
  };

  return (
    <div className="flex flex-col items-center gap-4">
      <Button size="lg" className="h-12 gap-2 rounded-full px-8 text-base" onClick={() => router.push("/trips")}>
        <Search className="size-4" />
        {t("browseAllJourneys")}
      </Button>
      <div className="flex flex-wrap justify-center gap-2">
        {QUICK_TIER_VALUES.map((value) => (
          <Button
            key={value ?? "all"}
            variant="outline"
            size="sm"
            className="rounded-full bg-background/60"
            onClick={() => router.push(value ? `/trips?tiers=${value}` : "/trips")}
          >
            {tierLabel(value)}
          </Button>
        ))}
      </div>
    </div>
  );
}
