"use client";

import { useRouter } from "next/navigation";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";

const QUICK_TIERS = [
  { label: "All journeys", value: undefined },
  { label: "VIP", value: "VIP" },
  { label: "Premium", value: "PREMIUM" },
  { label: "Economic", value: "ECONOMIC" },
];

export function HeroSearchCta() {
  const router = useRouter();

  return (
    <div className="flex flex-col items-center gap-4">
      <Button size="lg" className="h-12 gap-2 rounded-full px-8 text-base" onClick={() => router.push("/trips")}>
        <Search className="size-4" />
        Browse all journeys
      </Button>
      <div className="flex flex-wrap justify-center gap-2">
        {QUICK_TIERS.map((tier) => (
          <Button
            key={tier.label}
            variant="outline"
            size="sm"
            className="rounded-full bg-background/60"
            onClick={() => router.push(tier.value ? `/trips?tiers=${tier.value}` : "/trips")}
          >
            {tier.label}
          </Button>
        ))}
      </div>
    </div>
  );
}
