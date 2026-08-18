"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlidersHorizontal, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { today } from "@/lib/format/date";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetFooter,
  SheetTrigger,
  SheetClose,
} from "@/components/ui/sheet";

const TIER_VALUES = ["VIP", "PREMIUM", "ECONOMIC"] as const;

export function FilterBar() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const t = useTranslations("trips");
  const tTiers = useTranslations("tiers");
  const tRoomTypes = useTranslations("roomTypes");
  const activeTiers = searchParams.getAll("tiers");

  const TIERS = TIER_VALUES.map((value) => ({
    value,
    label: value === "VIP" ? tTiers("vip") : value === "PREMIUM" ? tTiers("premium") : tTiers("economic"),
  }));

  const [draft, setDraft] = useState({
    roomSize: searchParams.get("roomSize") ?? "",
    minPrice: searchParams.get("minPrice") ?? "",
    maxPrice: searchParams.get("maxPrice") ?? "",
    minDays: searchParams.get("minDays") ?? "",
    maxDays: searchParams.get("maxDays") ?? "",
    departureFrom: searchParams.get("departureFrom") ?? "",
    departureTo: searchParams.get("departureTo") ?? "",
  });

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

  function applyAdvanced() {
    pushParams((params) => {
      const keys = ["roomSize", "minPrice", "maxPrice", "minDays", "maxDays", "departureFrom", "departureTo"] as const;
      for (const key of keys) {
        const value = draft[key];
        if (value) params.set(key, value);
        else params.delete(key);
      }
    });
  }

  function clearAll() {
    setDraft({ roomSize: "", minPrice: "", maxPrice: "", minDays: "", maxDays: "", departureFrom: "", departureTo: "" });
    router.push("/trips");
  }

  const advancedCount = Object.values(draft).filter(Boolean).length;
  const hasAnyFilter = activeTiers.length > 0 || advancedCount > 0;

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

      <Sheet>
        <SheetTrigger render={<Button variant="outline" size="sm" className="rounded-full" />}>
          <SlidersHorizontal className="size-3.5" />
          {t("filtersButton")}
          {advancedCount > 0 && (
            <span className="ml-1 flex size-4 items-center justify-center rounded-full bg-primary text-[10px] text-primary-foreground">
              {advancedCount}
            </span>
          )}
        </SheetTrigger>
        <SheetContent side="right" className="w-full sm:max-w-sm">
          <SheetHeader>
            <SheetTitle>{t("filtersTitle")}</SheetTitle>
          </SheetHeader>
          <div className="flex flex-col gap-5 overflow-y-auto px-4">
            <div className="space-y-1.5">
              <Label>{t("roomSize")}</Label>
              <Select
                value={draft.roomSize}
                onValueChange={(v) => setDraft((d) => ({ ...d, roomSize: v ?? "" }))}
                items={[
                  { value: "1", label: tRoomTypes("single") },
                  { value: "2", label: tRoomTypes("double") },
                  { value: "3", label: tRoomTypes("triple") },
                  { value: "4", label: tRoomTypes("quad") },
                ]}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder={t("any")} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">{tRoomTypes("single")}</SelectItem>
                  <SelectItem value="2">{tRoomTypes("double")}</SelectItem>
                  <SelectItem value="3">{tRoomTypes("triple")}</SelectItem>
                  <SelectItem value="4">{tRoomTypes("quad")}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label>{t("priceRange")}</Label>
              <div className="flex items-center gap-2">
                <Input
                  type="number"
                  placeholder={t("min")}
                  value={draft.minPrice}
                  onChange={(e) => setDraft((d) => ({ ...d, minPrice: e.target.value }))}
                />
                <span className="text-muted-foreground">–</span>
                <Input
                  type="number"
                  placeholder={t("max")}
                  value={draft.maxPrice}
                  onChange={(e) => setDraft((d) => ({ ...d, maxPrice: e.target.value }))}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label>{t("tripLength")}</Label>
              <div className="flex items-center gap-2">
                <Input
                  type="number"
                  placeholder={t("min")}
                  value={draft.minDays}
                  onChange={(e) => setDraft((d) => ({ ...d, minDays: e.target.value }))}
                />
                <span className="text-muted-foreground">–</span>
                <Input
                  type="number"
                  placeholder={t("max")}
                  value={draft.maxDays}
                  onChange={(e) => setDraft((d) => ({ ...d, maxDays: e.target.value }))}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label>{t("departureWindow")}</Label>
              <div className="flex items-center gap-2">
                <Input
                  type="date"
                  min={today()}
                  value={draft.departureFrom}
                  onChange={(e) => setDraft((d) => ({ ...d, departureFrom: e.target.value }))}
                />
                <span className="text-muted-foreground">–</span>
                <Input
                  type="date"
                  min={today()}
                  value={draft.departureTo}
                  onChange={(e) => setDraft((d) => ({ ...d, departureTo: e.target.value }))}
                />
              </div>
            </div>
          </div>
          <SheetFooter>
            <SheetClose render={<Button onClick={applyAdvanced} />}>{t("applyFilters")}</SheetClose>
          </SheetFooter>
        </SheetContent>
      </Sheet>

      {hasAnyFilter && (
        <Button variant="ghost" size="sm" onClick={clearAll} className="text-muted-foreground">
          <X className="size-3.5" /> {t("clear")}
        </Button>
      )}
    </div>
  );
}
