"use client";

import { useTransition } from "react";
import { toast } from "sonner";
import { Copy, Loader2 } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { getTripDetailsAction, type TripDetail } from "@/lib/admin/trips-actions";
import { formatDate } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";

type Translator = (key: string, values?: Record<string, string | number>) => string;

const ROOM_TYPE_KEYS = {
  SINGLE: "single",
  DOUBLE: "double",
  TRIPLE: "triple",
  QUAD: "quad",
  CHILD: "child",
  INFANT: "infant",
} as const;

const TIER_KEYS = { VIP: "vip", PREMIUM: "premium", ECONOMIC: "economic" } as const;

function buildTripDetailsText(
  trip: TripDetail,
  locale: "ar" | "en",
  t: Translator,
  tStatus: Translator,
  tTier: Translator,
  tRoom: Translator,
  tForm: Translator,
): string {
  const none = t("noneValue");
  const lines: string[] = [];

  lines.push(t("header", { title: trip.title ?? "", tripCode: trip.tripCode ?? "" }));
  lines.push(`${t("labelCompany")}: ${trip.company?.companyName ?? none}`);
  lines.push(`${t("labelStatus")}: ${trip.status ? tStatus(trip.status) : none}`);
  lines.push(`${t("labelTier")}: ${trip.tier ? tTier(TIER_KEYS[trip.tier]) : none}`);
  lines.push(
    `${t("labelDates")}: ${formatDate(trip.departureDate, undefined, locale)} — ${formatDate(trip.returnDate, undefined, locale)}`,
  );
  if (trip.daysInMakkah != null || trip.daysInMadinah != null) {
    lines.push(t("labelNights", { makkah: trip.daysInMakkah ?? 0, madinah: trip.daysInMadinah ?? 0 }));
  }
  lines.push(`${t("labelSeats")}: ${trip.availableSeats ?? none}`);

  lines.push("");
  lines.push(`${t("labelAirline")}: ${trip.airline ?? none}`);
  if (trip.outboundDepartureAirport || trip.outboundArrivalAirport) {
    lines.push(
      `${t("labelOutbound")}: ${trip.outboundDepartureAirport?.iataCode ?? "?"} → ${trip.outboundArrivalAirport?.iataCode ?? "?"}`,
    );
  }
  if (trip.returnDepartureAirport || trip.returnArrivalAirport) {
    lines.push(
      `${t("labelReturn")}: ${trip.returnDepartureAirport?.iataCode ?? "?"} → ${trip.returnArrivalAirport?.iataCode ?? "?"}`,
    );
  }

  function hotelLine(entry: NonNullable<TripDetail["hotels"]>[number] | undefined): string {
    const h = entry?.hotel;
    if (!h) return none;
    return t("hotelLine", { name: h.name ?? "", stars: h.stars ?? 0, distance: h.distanceToHaramM ?? 0 });
  }
  const makkahHotel = trip.hotels?.find((h) => h.city === "MAKKAH");
  const madinahHotel = trip.hotels?.find((h) => h.city === "MADINAH");
  if (makkahHotel || madinahHotel) {
    lines.push("");
    if (makkahHotel) lines.push(`${t("labelHotelMakkah")}: ${hotelLine(makkahHotel)}`);
    if (madinahHotel) lines.push(`${t("labelHotelMadinah")}: ${hotelLine(madinahHotel)}`);
  }

  if (trip.roomPrices && trip.roomPrices.length > 0) {
    lines.push("");
    lines.push(`${t("labelRoomPrices")}:`);
    for (const p of trip.roomPrices) {
      if (!p.roomType) continue;
      lines.push(`  - ${tRoom(ROOM_TYPE_KEYS[p.roomType])}: ${formatMoney(p.price, trip.currency, locale)}`);
    }
  }

  const inclusions: string[] = [];
  if (trip.visaIncluded) inclusions.push(tForm("inclusionVisa"));
  if (trip.transportationIncluded) inclusions.push(tForm("inclusionTransportation"));
  if (trip.mealsIncluded) inclusions.push(tForm("inclusionMeals"));
  if (trip.guideIncluded) inclusions.push(tForm("inclusionGuide"));
  if (trip.zamzamIncluded) inclusions.push(tForm("inclusionZamzam"));
  if (trip.fastTrainIncluded) inclusions.push(tForm("inclusionFastTrain"));
  if (inclusions.length > 0) {
    lines.push("");
    lines.push(`${t("labelInclusions")}: ${inclusions.join(locale === "ar" ? "، " : ", ")}`);
  }

  lines.push("");
  lines.push(`${t("labelCommission")}: ${formatMoney(trip.commissionPerTraveler, trip.currency, locale)}`);
  lines.push(`${t("labelCashback")}: ${formatMoney(trip.cashbackPerTraveler, trip.currency, locale)}`);

  if (trip.description) {
    lines.push("");
    lines.push(`${t("labelDescription")}: ${trip.description}`);
  }

  return lines.join("\n");
}

export function CopyTripDetailsButton({ tripId, compact = true }: { tripId: string; compact?: boolean }) {
  const locale = useLocale() as "ar" | "en";
  const t = useTranslations("admin.trips.copy");
  const tCommon = useTranslations("admin.common");
  const tStatus = useTranslations("admin.tripStatus");
  const tTier = useTranslations("tiers");
  const tRoom = useTranslations("roomTypes");
  const tForm = useTranslations("admin.trips.form");
  const [pending, startTransition] = useTransition();

  function handleClick() {
    startTransition(async () => {
      const result = await getTripDetailsAction(tripId);
      if (!result.ok || !result.data) {
        toast.error(result.ok ? tCommon("somethingWentWrong") : result.error);
        return;
      }
      const text = buildTripDetailsText(result.data, locale, t, tStatus, tTier, tRoom, tForm);
      try {
        await navigator.clipboard.writeText(text);
        toast.success(tCommon("tripDetailsCopied"));
      } catch {
        toast.error(tCommon("copyFailed"));
      }
    });
  }

  const icon = pending ? <Loader2 className="size-4 animate-spin" /> : <Copy className="size-4" />;

  if (!compact) {
    return (
      <Button variant="outline" disabled={pending} onClick={handleClick}>
        {icon} {tCommon("copyTripDetails")}
      </Button>
    );
  }

  return (
    <Tooltip>
      <TooltipTrigger
        render={
          <Button
            variant="ghost"
            size="icon"
            disabled={pending}
            onClick={handleClick}
            aria-label={tCommon("copyTripDetails")}
          />
        }
      >
        {icon}
      </TooltipTrigger>
      <TooltipContent>{tCommon("copyTripDetails")}</TooltipContent>
    </Tooltip>
  );
}
