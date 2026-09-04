"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import type { CityOption } from "@/lib/admin/cities";
import type { AirportOption } from "@/lib/admin/reference-data";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ADVANCED_FILTER_KEYS, AdvancedFilterSheet } from "@/components/trip/advanced-filter-sheet";

const TIER_VALUES = ["VIP", "PREMIUM", "ECONOMIC"] as const;

export function FilterBar({ cities = [], airports = [] }: { cities?: CityOption[]; airports?: AirportOption[] }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const t = useTranslations("trips");
  const tTiers = useTranslations("tiers");
  const activeTiers = searchParams.getAll("tiers");
  const priceSort = searchParams.get("priceSort") ?? "";
  const durationSort = searchParams.get("durationSort") ?? "";

  const TIERS = TIER_VALUES.map((value) => ({
    value,
    label: value === "VIP" ? tTiers("vip") : value === "PREMIUM" ? tTiers("premium") : tTiers("economic"),
  }));

  function pushParams(mutate: (params: URLSearchParams) => void) {
    const params = new URLSearchParams(searchParams.toString());
    mutate(params);
    params.delete("page");
    router.push(`/trips?${params.toString()}`);
  }

  function toggleTier(tier: string) {
    pushParams((params) => {
      const current = params.getAll("tiers");
      params.delete("tiers");
      const next = current.includes(tier) ? current.filter((t) => t !== tier) : [...current, tier];
      next.forEach((t) => params.append("tiers", t));
    });
  }

  function setPriceSort(direction: string) {
    pushParams((params) => {
      if (direction) params.set("priceSort", direction);
      else params.delete("priceSort");
    });
  }

  function setDurationSort(direction: string) {
    pushParams((params) => {
      if (direction) params.set("durationSort", direction);
      else params.delete("durationSort");
    });
  }

  function clearAll() {
    router.push("/trips");
  }

  const hasAnyFilter =
    activeTiers.length > 0 ||
    Boolean(priceSort) ||
    Boolean(durationSort) ||
    ADVANCED_FILTER_KEYS.some((key) => Boolean(searchParams.get(key)));

  return (
    <div className="flex flex-wrap items-center gap-2">
      {TIERS.map((tier) => (
        <Badge
          key={tier.value}
          variant={activeTiers.includes(tier.value) ? "default" : "outline"}
          className="cursor-pointer rounded-full px-3 py-1.5 text-sm"
          onClick={() => toggleTier(tier.value)}
        >
          {tier.label}
        </Badge>
      ))}

      {/* Price sort is the most important sort criterion, so it's a primary control here — applied
          immediately, not tucked into the advanced-filters Sheet below. */}
      <Select
        value={priceSort}
        onValueChange={(v) => setPriceSort(v ?? "")}
        items={[
          { value: "asc", label: t("sortPriceAsc") },
          { value: "desc", label: t("sortPriceDesc") },
        ]}
      >
        <SelectTrigger className="w-auto rounded-full" size="sm">
          <SelectValue placeholder={t("sortBy")} />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="asc">{t("sortPriceAsc")}</SelectItem>
          <SelectItem value="desc">{t("sortPriceDesc")}</SelectItem>
        </SelectContent>
      </Select>

      {/* Trip-length sort is the secondary tie-breaker when combined with price sort — see
          TripSpecifications.orderBrowseResults on the backend. */}
      <Select
        value={durationSort}
        onValueChange={(v) => setDurationSort(v ?? "")}
        items={[
          { value: "asc", label: t("sortDurationAsc") },
          { value: "desc", label: t("sortDurationDesc") },
        ]}
      >
        <SelectTrigger className="w-auto rounded-full" size="sm">
          <SelectValue placeholder={t("sortBy")} />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="asc">{t("sortDurationAsc")}</SelectItem>
          <SelectItem value="desc">{t("sortDurationDesc")}</SelectItem>
        </SelectContent>
      </Select>

      <AdvancedFilterSheet pathname="/trips" cities={cities} airports={airports} />

      {hasAnyFilter && (
        <Button variant="ghost" size="sm" onClick={clearAll} className="text-muted-foreground">
          <X className="size-3.5" /> {t("clear")}
        </Button>
      )}
    </div>
  );
}
