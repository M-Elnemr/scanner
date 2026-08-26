import type { Metadata } from "next";
import { cache } from "react";
import { notFound } from "next/navigation";
import {
  BadgeCheck,
  Bus,
  CalendarDays,
  Car,
  LocateFixed,
  MapPin,
  ShieldCheck,
  Soup,
  Sparkles,
  TrainFront,
  UserRound,
} from "lucide-react";
import { getLocale, getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { getCurrentUser } from "@/lib/auth/server";
import { cn } from "@/lib/utils";
import { formatDate } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";
import { HotelPhotoCarousel } from "@/components/trip/hotel-photo-carousel";
import { PreserveFlow } from "@/components/trip/preserve-flow";
import { FavouriteButton } from "@/components/trip/favourite-button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";

/** Deduped across generateMetadata + the page body — both need the same trip in one request. */
const getTrip = cache(async (id: string) => {
  const api = await apiClient();
  return api.GET("/api/v1/trips/{id}", { params: { path: { id } }, cache: "no-store" });
});

export async function generateMetadata(props: PageProps<"/trips/[id]">): Promise<Metadata> {
  const { id } = await props.params;
  const [result, t] = await Promise.all([getTrip(id), getTranslations("tripDetail")]);
  return { title: result.data?.data?.title ?? t("metaFallbackTitle") };
}

export default async function TripDetailPage(props: PageProps<"/trips/[id]">) {
  const { id } = await props.params;
  const [user, api, t, tRoomTypes, tTiers, locale] = await Promise.all([
    getCurrentUser(),
    apiClient(),
    getTranslations("tripDetail"),
    getTranslations("roomTypes"),
    getTranslations("tiers"),
    getLocale() as Promise<"ar" | "en">,
  ]);

  const ROOM_LABEL: Record<string, string> = {
    SINGLE: tRoomTypes("single"),
    DOUBLE: tRoomTypes("double"),
    TRIPLE: tRoomTypes("triple"),
    QUAD: tRoomTypes("quad"),
    QUINT: tRoomTypes("quint"),
    CHILD: tRoomTypes("child"),
    INFANT: tRoomTypes("infant"),
  };

  const TIER_LABEL: Record<string, string> = { VIP: tTiers("vip"), PREMIUM: tTiers("premium"), ECONOMIC: tTiers("economic") };

  const isCustomer = user?.role === "CUSTOMER";
  const [result, activeResult, favResult] = await Promise.all([
    getTrip(id),
    isCustomer ? api.GET("/api/v1/customers/me/leads/active", { cache: "no-store" }) : Promise.resolve(null),
    isCustomer
      ? api.GET("/api/v1/customers/me/favourites", { params: { query: { page: 0, size: 200 } as never }, cache: "no-store" })
      : Promise.resolve(null),
  ]);
  const trip = result.data?.data;
  if (!trip) notFound();

  let activeLead: { leadId: string; tripId: string; tripTitle: string } | null = null;
  let favourited = false;
  if (isCustomer) {
    const active = activeResult?.data?.data;
    if (active?.id && active.tripId && active.tripTitle) {
      activeLead = { leadId: active.id, tripId: active.tripId, tripTitle: active.tripTitle };
    }
    favourited = Boolean(favResult?.data?.data?.content?.some((f) => f.trip?.id === id));
  }

  const inclusions = [
    { active: trip.visaIncluded, icon: ShieldCheck, label: t("inclusionVisa") },
    { active: trip.transportationIncluded, icon: Car, label: t("inclusionTransport") },
    { active: trip.mealsIncluded, icon: Soup, label: t("inclusionMeals") },
    { active: trip.guideIncluded, icon: UserRound, label: t("inclusionGuide") },
    { active: trip.zamzamIncluded, icon: Sparkles, label: t("inclusionZamzam") },
    { active: trip.fastTrainIncluded, icon: TrainFront, label: t("inclusionHaramainTrain") },
  ].filter((i) => i.active);

  // Makkah first — the more important leg of the journey, so it sits on the reading-start side
  // (the right in Arabic) rather than wherever the backend happened to return it.
  const sortedHotels = [...(trip.hotels ?? [])].sort((a, b) => (a.city === "MAKKAH" ? -1 : b.city === "MAKKAH" ? 1 : 0));
  const hotelPhotos = sortedHotels.map((th) => ({
    city: th.city,
    hotelName: th.hotel?.name,
    hotelNameAr: th.hotel?.nameAr,
    photoUrl: th.hotel?.photoUrl,
  }));
  const makkahGroup = sortedHotels.filter((th) => th.city === "MAKKAH");
  const madinahGroup = sortedHotels.filter((th) => th.city === "MADINAH");

  const roomPriceValues = (trip.roomPrices ?? []).map((p) => p.price).filter((p): p is number => p != null);
  const quadPrice = trip.roomPrices?.find((p) => p.roomType === "QUAD")?.price;
  const startingPrice = quadPrice ?? (roomPriceValues.length ? Math.min(...roomPriceValues) : undefined);

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="grid gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          <div className="relative h-56 overflow-hidden rounded-2xl sm:h-72">
            <HotelPhotoCarousel photos={hotelPhotos} tier={trip.tier} locale={locale} variant="detail" className="h-full w-full" />
            {trip.tier && (
              <Badge className="absolute top-4 left-4 bg-white/90 text-foreground" variant="secondary">
                {TIER_LABEL[trip.tier] ?? trip.tier}
              </Badge>
            )}
          </div>

          <div>
            <div className="flex items-start justify-between gap-3">
              <h1 className="font-heading text-3xl font-bold tracking-tight text-balance">{trip.title}</h1>
              {user?.role === "CUSTOMER" && trip.id && (
                <FavouriteButton tripId={trip.id} initialFavourited={favourited} />
              )}
            </div>
            <p className="mt-1 text-sm text-muted-foreground">{t("tripCode", { code: trip.tripCode ?? "" })}</p>
          </div>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Stat icon={CalendarDays} label={t("departs")} value={formatDate(trip.departureDate, undefined, locale)} />
            <Stat icon={CalendarDays} label={t("returns")} value={formatDate(trip.returnDate, undefined, locale)} />
            <Stat icon={MapPin} label={t("makkah")} value={t("nightsCount", { count: trip.daysInMakkah ?? 0 })} />
            <Stat icon={MapPin} label={t("madinah")} value={t("nightsCount", { count: trip.daysInMadinah ?? 0 })} />
          </div>

          <Card>
            <CardContent className="space-y-3 pt-6">
              <h2 className="font-heading text-lg font-semibold">{t("itinerary")}</h2>
              <RouteLeg
                label={t("outbound")}
                from={trip.outboundDepartureAirport}
                to={trip.outboundArrivalAirport}
                airline={trip.airline}
                locale={locale}
              />
              <RouteLeg
                label={t("return")}
                from={trip.returnDepartureAirport}
                to={trip.returnArrivalAirport}
                airline={trip.airline}
                locale={locale}
              />
            </CardContent>
          </Card>

          {inclusions.length > 0 && (
            <div>
              <h2 className="mb-3 font-heading text-lg font-semibold">{t("whatsIncluded")}</h2>
              <div className="flex flex-wrap gap-2">
                {inclusions.map((inc) => (
                  <span
                    key={inc.label}
                    className="flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1.5 text-sm text-primary"
                  >
                    <inc.icon className="size-3.5" /> {inc.label}
                  </span>
                ))}
              </div>
            </div>
          )}

          {sortedHotels.length ? (
            <div>
              <h2 className="mb-3 font-heading text-lg font-semibold">{t("hotels")}</h2>
              {/* Each city gets the full content width (not a 2-col grid) so its hotel alternatives
                  have room to sit beside each other instead of wrapping back to a vertical stack. */}
              <div className="space-y-4">
                {makkahGroup.length > 0 && <HotelCityCard city="MAKKAH" hotels={makkahGroup} t={t} locale={locale} />}
                {madinahGroup.length > 0 && <HotelCityCard city="MADINAH" hotels={madinahGroup} t={t} locale={locale} />}
              </div>
            </div>
          ) : null}

          {trip.roomPrices?.length ? (
            <div>
              <h2 className="mb-3 font-heading text-lg font-semibold">{t("roomPrices")}</h2>
              <div className="overflow-hidden rounded-xl border border-border">
                <table className="w-full text-sm">
                  <tbody>
                    {[...trip.roomPrices].sort((a, b) => roomTypeSortOrder(a.roomType) - roomTypeSortOrder(b.roomType)).map((p, i) => (
                      <tr key={i} className="border-b border-border last:border-0 even:bg-secondary/30">
                        <td className="p-3">{p.roomType ? (ROOM_LABEL[p.roomType] ?? p.roomType) : "—"}</td>
                        <td className="p-3 text-right font-semibold">{formatMoney(p.price, trip.currency, locale)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}

          {trip.description && (
            <div>
              <h2 className="mb-2 font-heading text-lg font-semibold">{t("aboutJourney")}</h2>
              <p className="whitespace-pre-line text-muted-foreground">{trip.description}</p>
            </div>
          )}
        </div>

        <div className="lg:sticky lg:top-24 lg:h-fit">
          <Card className="border-2 border-primary/15 shadow-lg">
            <CardContent className="space-y-4 pt-6">
              {startingPrice != null && (
                <div>
                  <p className="text-xs font-medium text-muted-foreground">{t("startingFrom")}</p>
                  <p className="font-heading text-3xl font-bold text-primary">
                    {formatMoney(startingPrice, trip.currency, locale)}
                  </p>
                </div>
              )}
              {trip.availableSeats != null && (
                <p
                  className={cn(
                    "text-sm font-medium",
                    trip.availableSeats <= 5 ? "text-destructive" : "text-muted-foreground",
                  )}
                >
                  {t("seatsLeft", { count: trip.availableSeats })}
                </p>
              )}
              {trip.id && (
                <PreserveFlow
                  tripId={trip.id}
                  tripTitle={trip.title ?? t("thisTripFallback")}
                  isLoggedIn={Boolean(user)}
                  canPreserve={!user || user.role === "CUSTOMER"}
                  activeLead={activeLead}
                />
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

// Fixed landmark, used as the reference point on a Madinah hotel's "view on map" link.
const PROPHETS_MOSQUE = { lat: 24.4672, lng: 39.6112 };

/**
 * Makkah's Masjid al-Haram is large enough that routing every hotel to the Kaaba's own coordinates
 * produces a misleading walking route — a hotel east of the mosque sends guests to an eastern gate
 * in real life, not to the geometric center. These 11 major gates (OpenStreetMap `entrance` nodes,
 * cross-checked against their well-known gate numbers: 1 = King Abdul Aziz, 79 = King Fahd,
 * 100 = King Abdullah, 40 = Umrah Gate) span the full perimeter, so picking whichever is nearest a
 * given hotel and routing there instead gives Google's own directions a realistic starting point.
 */
const HARAM_GATES = [
  { name: "King Abdul Aziz Gate", nameAr: "بوابة الملك عبدالعزيز", lat: 21.4210447, lng: 39.8258406 },
  { name: "King Fahd Gate", nameAr: "بوابة الملك فهد", lat: 21.421232, lng: 39.8240461 },
  { name: "King Abdullah Gate", nameAr: "بوابة الملك عبدالله", lat: 21.4253914, lng: 39.824132 },
  { name: "Umrah Gate", nameAr: "بوابة العمرة", lat: 21.4228378, lng: 39.8245696 },
  { name: "Al Fath Gate", nameAr: "بوابة الفتح", lat: 21.4240151, lng: 39.8265147 },
  { name: "Bab Al Marwah", nameAr: "باب المروة", lat: 21.4253183, lng: 39.8273872 },
  { name: "Ajyad Gate", nameAr: "بوابة أجياد", lat: 21.4213059, lng: 39.8268532 },
  { name: "Al Quds Gate", nameAr: "بوابة القدس", lat: 21.4238175, lng: 39.8252692 },
  { name: "Bab Al Salam", nameAr: "باب السلام", lat: 21.4230322, lng: 39.8275662 },
  { name: "Al Nabi Gate", nameAr: "بوابة النبي", lat: 21.4222008, lng: 39.8276781 },
  { name: "Bab Ali", nameAr: "باب علي", lat: 21.4229524, lng: 39.8275792 },
] as const;

function distanceMeters(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function nearestGate(lat: number, lng: number) {
  return HARAM_GATES.reduce((closest, gate) =>
    distanceMeters(lat, lng, gate.lat, gate.lng) < distanceMeters(lat, lng, closest.lat, closest.lng) ? gate : closest,
  );
}

/** Coordinates win whenever we have them — they let us route to the nearest Haram gate / the
 * Prophet's Mosque and show a real distance, which is the whole point of this link. A pasted maps
 * link is only a fallback for hotels we don't have coordinates for; it's just a bare pin otherwise. */
function mapUrl(
  hotel: { locationUrl?: string | null; latitude?: number | null; longitude?: number | null } | null | undefined,
  city: string | undefined,
): string | undefined {
  if (!hotel) return undefined;
  if (hotel.latitude != null && hotel.longitude != null) {
    const reference = city === "MADINAH" ? PROPHETS_MOSQUE : nearestGate(hotel.latitude, hotel.longitude);
    const params = new URLSearchParams({
      api: "1",
      origin: `${hotel.latitude},${hotel.longitude}`,
      destination: `${reference.lat},${reference.lng}`,
    });
    return `https://www.google.com/maps/dir/?${params.toString()}`;
  }
  return hotel.locationUrl ?? undefined;
}

/** Names the specific gate being routed to for a Makkah hotel with real coordinates; the generic
 * label otherwise (Madinah, or a hotel with no coordinates falling back to a bare pasted link). */
function mapLinkLabel(
  hotel: { locationUrl?: string | null; latitude?: number | null; longitude?: number | null } | null | undefined,
  city: string | undefined,
  locale: "ar" | "en",
  t: (key: string, values?: Record<string, string | number>) => string,
): string {
  if (city === "MAKKAH" && hotel?.latitude != null && hotel?.longitude != null) {
    const gate = nearestGate(hotel.latitude, hotel.longitude);
    return t("viewOnMapVia", { gate: locale === "ar" ? gate.nameAr : gate.name });
  }
  return t("viewOnMap");
}

type TripHotelItem = {
  city?: string;
  hotel?: {
    name?: string;
    nameAr?: string;
    stars?: number;
    distanceToHaramM?: number | null;
    canWalk?: boolean;
    freeBusIncluded?: boolean;
    locationUrl?: string | null;
    latitude?: number | null;
    longitude?: number | null;
  };
};

/**
 * One card per hotel, grouped under a single city label. Each alternative gets its own full detail
 * (stars, distance, walkability, map link, free shuttle) instead of collapsing multiple hotels'
 * names into one line — that used to drop distance/map for anything but a single hotel per city,
 * and joined star-rating glyphs directly against the next hotel's name/"or" text with no real
 * whitespace between them (readable via CSS margins, but broken wherever styling doesn't apply).
 */
function HotelCityCard({
  city,
  hotels,
  t,
  locale,
}: {
  city: "MAKKAH" | "MADINAH";
  hotels: TripHotelItem[];
  t: (key: string, values?: Record<string, string | number>) => string;
  locale: "ar" | "en";
}) {
  const cityLabel = city === "MAKKAH" ? t("makkah") : t("madinah");

  return (
    <div className="space-y-3">
      <CityBadge label={cityLabel} />
      <div className="flex flex-wrap items-stretch gap-3">
        {hotels.map((h, i) => (
          <div key={i} className="flex items-stretch gap-3">
            {i > 0 && <span className="self-center text-xs font-medium text-muted-foreground">{t("orSeparator")}</span>}
            <HotelDetailCard hotel={h.hotel} city={city} locale={locale} t={t} className="flex-1 min-w-[220px]" />
          </div>
        ))}
      </div>
    </div>
  );
}

/** Full detail for a single hotel — everything except photos, which the carousel above already covers. */
function HotelDetailCard({
  hotel,
  city,
  locale,
  t,
  className,
}: {
  hotel: TripHotelItem["hotel"];
  city: "MAKKAH" | "MADINAH";
  locale: "ar" | "en";
  t: (key: string, values?: Record<string, string | number>) => string;
  className?: string;
}) {
  const name = locale === "ar" && hotel?.nameAr ? hotel.nameAr : hotel?.name;
  const link = mapUrl(hotel, city);
  const hasFacts = hotel?.distanceToHaramM != null || Boolean(link) || Boolean(hotel?.freeBusIncluded);

  return (
    <Card className={className}>
      <CardContent className="space-y-3 pt-6">
        <div>
          <p className="font-heading font-semibold">{name}</p>
          {hotel?.stars ? (
            <div className="mt-1 flex items-center gap-1 text-amber-500">
              {Array.from({ length: hotel.stars }).map((_, j) => (
                <BadgeCheck key={j} className="size-3.5" />
              ))}
            </div>
          ) : null}
        </div>

        {/* Facts row is optional per hotel — a hotel with no coordinates/map link (and no shuttle or
            distance) just skips straight from name+stars to the next card, no empty gap left behind. */}
        {hasFacts && (
          <div className="flex flex-wrap gap-1.5 border-t border-border pt-3">
            {hotel?.distanceToHaramM != null && (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-2.5 py-1 text-sm font-medium text-primary">
                <MapPin className="size-3.5 shrink-0" />
                {t("distanceFrom", { distance: hotel.distanceToHaramM, place: city === "MAKKAH" ? t("haram") : t("prophetsMosque") })}
                {hotel.canWalk ? t("walkableSuffix") : ""}
              </span>
            )}
            {link && (
              <a
                href={link}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-2.5 py-1 text-sm font-medium text-primary transition-colors hover:bg-primary/20"
              >
                <LocateFixed className="size-3.5 shrink-0" /> {mapLinkLabel(hotel, city, locale, t)}
              </a>
            )}
            {hotel?.freeBusIncluded && (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-2.5 py-1 text-sm font-medium text-primary">
                <Bus className="size-3.5 shrink-0" /> {t("freeShuttle")}
              </span>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function CityBadge({ label }: { label: string }) {
  return (
    <span className="inline-block rounded-full bg-primary/10 px-2.5 py-1 text-xs font-bold tracking-wide text-primary uppercase">
      {label}
    </span>
  );
}

const ROOM_TYPE_ORDER: Record<string, number> = { DOUBLE: 0, TRIPLE: 1, QUAD: 2, QUINT: 3, CHILD: 4, INFANT: 5, SINGLE: 6 };

function roomTypeSortOrder(roomType: string | undefined): number {
  return roomType != null && roomType in ROOM_TYPE_ORDER ? ROOM_TYPE_ORDER[roomType] : 99;
}

function Stat({ icon: Icon, label, value }: { icon: typeof CalendarDays; label: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-3">
      <Icon className="mb-1 size-4 text-primary" />
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm font-semibold">{value}</p>
    </div>
  );
}

function RouteLeg({
  label,
  from,
  to,
  airline,
  locale,
}: {
  label: string;
  from?: { iataCode?: string; city?: string; cityAr?: string };
  to?: { iataCode?: string; city?: string; cityAr?: string };
  airline?: string;
  locale: "ar" | "en";
}) {
  const cityLabel = (airport?: { iataCode?: string; city?: string; cityAr?: string }) =>
    (locale === "ar" && airport?.cityAr ? airport.cityAr : airport?.city) ?? airport?.iataCode;

  return (
    <div className="flex items-center justify-between text-sm">
      <span className="w-16 shrink-0 text-xs font-medium text-muted-foreground">{label}</span>
      <span className="flex-1 text-right">{cityLabel(from)}</span>
      <span className="mx-3 text-muted-foreground">✈ {airline}</span>
      <span className="flex-1">{cityLabel(to)}</span>
    </div>
  );
}
