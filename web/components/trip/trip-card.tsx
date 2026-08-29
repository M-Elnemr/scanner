"use client";

import Link from "next/link";
import { CalendarDays, MapPin } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { HotelPhotoCarousel } from "@/components/trip/hotel-photo-carousel";
import { CompareToggle } from "@/components/trip/compare-toggle";
import { Badge } from "@/components/ui/badge";
import { formatDate, tripDurationDays } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";
import type { components } from "@/lib/api/schema";

type TripSummary = components["schemas"]["TripSummaryResponse"];

export function TripCard({ trip }: { trip: TripSummary }) {
  const t = useTranslations("trips");
  const tTiers = useTranslations("tiers");
  const locale = useLocale() as "ar" | "en";
  const days = tripDurationDays(trip.departureDate, trip.returnDate);

  const TIER_LABEL: Record<string, string> = { VIP: tTiers("vip"), PREMIUM: tTiers("premium"), ECONOMIC: tTiers("economic") };

  return (
    <Link
      href={`/trips/${trip.id}`}
      className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-card transition-all hover:-translate-y-1 hover:shadow-lg"
    >
      <div className="relative h-40">
        <HotelPhotoCarousel
          photos={trip.hotelPhotos ?? []}
          tier={trip.tier}
          locale={locale}
          variant="card"
          className="h-full w-full"
        />
        {trip.tier && (
          <Badge className="absolute top-3 left-3 bg-white/90 text-foreground shadow-sm" variant="secondary">
            {TIER_LABEL[trip.tier] ?? trip.tier}
          </Badge>
        )}
        {trip.id && <CompareToggle tripId={trip.id} />}
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4">
        <h3 className="line-clamp-2 font-heading text-base font-semibold leading-snug group-hover:text-primary">
          {trip.title}
        </h3>

        <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <CalendarDays className="size-3.5" />
            {formatDate(trip.departureDate, undefined, locale)}
            {days != null ? ` · ${t("daysAbbrev", { count: days })}` : ""}
          </span>
          <span className="flex items-center gap-1">
            <MapPin className="size-3.5" />
            {trip.outboundDepartureAirport?.iataCode} → {trip.outboundArrivalAirport?.iataCode}
          </span>
        </div>

        <div className="mt-auto flex items-end justify-between pt-2">
          <div>
            <p className="text-[11px] text-muted-foreground">{t("from")}</p>
            <p className="font-heading text-lg font-bold text-primary">
              {formatMoney(trip.priceStartsFrom, trip.currency, locale)}
            </p>
          </div>
        </div>
      </div>
    </Link>
  );
}
