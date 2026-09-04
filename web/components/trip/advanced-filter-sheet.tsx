"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlidersHorizontal } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { today } from "@/lib/format/date";
import type { CityOption } from "@/lib/admin/cities";
import type { AirportOption } from "@/lib/admin/reference-data";
import { Button } from "@/components/ui/button";
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

export const ADVANCED_FILTER_KEYS = [
  "roomSize",
  "minPrice",
  "maxPrice",
  "minDays",
  "maxDays",
  "departureFrom",
  "departureTo",
  "cityId",
  "departureAirportId",
] as const;

type AdvancedFilterDraft = Record<(typeof ADVANCED_FILTER_KEYS)[number], string>;

function airportLabel(a: AirportOption, locale: "ar" | "en") {
  const city = locale === "ar" && a.cityAr ? a.cityAr : a.city;
  return `${city} (${a.iataCode})`;
}

/**
 * The room size / price / trip length / company city / departure airport / departure window
 * filter Sheet — shared by the public `/trips` filter bar (`FilterBar`) and the admin trips
 * filter entry point, so the two can never drift on what "advanced filters" means. Owns its own
 * draft state (seeded from whatever's already in the URL) and, on Apply, merges just these keys
 * into the current search params and pushes to `pathname` — every other param already on the URL
 * (tiers, sort, or an admin-only status/company/search filter) is preserved untouched.
 */
export function AdvancedFilterSheet({
  pathname,
  cities = [],
  airports = [],
}: {
  pathname: string;
  cities?: CityOption[];
  airports?: AirportOption[];
}) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const locale = useLocale() as "ar" | "en";
  const t = useTranslations("trips");
  const tRoomTypes = useTranslations("roomTypes");

  const [draft, setDraft] = useState<AdvancedFilterDraft>({
    roomSize: searchParams.get("roomSize") ?? "",
    minPrice: searchParams.get("minPrice") ?? "",
    maxPrice: searchParams.get("maxPrice") ?? "",
    minDays: searchParams.get("minDays") ?? "",
    maxDays: searchParams.get("maxDays") ?? "",
    departureFrom: searchParams.get("departureFrom") ?? "",
    departureTo: searchParams.get("departureTo") ?? "",
    cityId: searchParams.get("cityId") ?? "",
    departureAirportId: searchParams.get("departureAirportId") ?? "",
  });

  function applyAdvanced() {
    const params = new URLSearchParams(searchParams.toString());
    for (const key of ADVANCED_FILTER_KEYS) {
      const value = draft[key];
      if (value) params.set(key, value);
      else params.delete(key);
    }
    params.delete("page");
    router.push(`${pathname}?${params.toString()}`);
  }

  const advancedCount = Object.values(draft).filter(Boolean).length;

  return (
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
                { value: "5", label: tRoomTypes("quint") },
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
                <SelectItem value="5">{tRoomTypes("quint")}</SelectItem>
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
            <Label>{t("companyCity")}</Label>
            <Select
              value={draft.cityId}
              onValueChange={(v) => setDraft((d) => ({ ...d, cityId: v ?? "" }))}
              items={cities.map((c) => ({ value: c.id, label: locale === "ar" && c.nameAr ? c.nameAr : c.name }))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder={t("anyCity")} />
              </SelectTrigger>
              <SelectContent>
                {cities.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {locale === "ar" && c.nameAr ? c.nameAr : c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label>{t("departureAirport")}</Label>
            <Select
              value={draft.departureAirportId}
              onValueChange={(v) => setDraft((d) => ({ ...d, departureAirportId: v ?? "" }))}
              items={airports.map((a) => ({ value: a.id, label: airportLabel(a, locale) }))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder={t("anyAirport")} />
              </SelectTrigger>
              <SelectContent>
                {airports.map((a) => (
                  <SelectItem key={a.id} value={a.id}>
                    {airportLabel(a, locale)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
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
  );
}
