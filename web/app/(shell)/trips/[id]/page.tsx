import type { Metadata } from "next";
import { notFound } from "next/navigation";
import {
  BadgeCheck,
  Bus,
  CalendarDays,
  Car,
  Lock,
  MapPin,
  ShieldCheck,
  Soup,
  Sparkles,
  TrainFront,
  UserRound,
} from "lucide-react";
import { getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { getCurrentUser } from "@/lib/auth/server";
import { ApiError } from "@/lib/api/errors";
import { formatDate } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";
import { TripImage } from "@/components/trip/trip-image";
import { CompanyCard } from "@/components/trip/company-card";
import { PreserveFlow } from "@/components/trip/preserve-flow";
import { FavouriteButton } from "@/components/trip/favourite-button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";

export async function generateMetadata(props: PageProps<"/trips/[id]">): Promise<Metadata> {
  const { id } = await props.params;
  const api = await apiClient();
  const [result, t] = await Promise.all([
    api.GET("/api/v1/trips/{id}", { params: { path: { id } } }),
    getTranslations("tripDetail"),
  ]);
  return { title: result.data?.data?.title ?? t("metaFallbackTitle") };
}

export default async function TripDetailPage(props: PageProps<"/trips/[id]">) {
  const { id } = await props.params;
  const [user, api, t, tRoomTypes, tTiers] = await Promise.all([
    getCurrentUser(),
    apiClient(),
    getTranslations("tripDetail"),
    getTranslations("roomTypes"),
    getTranslations("tiers"),
  ]);

  const ROOM_LABEL: Record<string, string> = {
    SINGLE: tRoomTypes("single"),
    DOUBLE: tRoomTypes("double"),
    TRIPLE: tRoomTypes("triple"),
    QUAD: tRoomTypes("quad"),
    CHILD: tRoomTypes("child"),
    INFANT: tRoomTypes("infant"),
  };

  const TIER_LABEL: Record<string, string> = { VIP: tTiers("vip"), PREMIUM: tTiers("premium"), ECONOMIC: tTiers("economic") };

  const result = await api.GET("/api/v1/trips/{id}", { params: { path: { id } }, cache: "no-store" });
  const trip = result.data?.data;
  if (!trip) notFound();

  let activeLead: { leadId: string; tripId: string; tripTitle: string } | null = null;
  let favourited = false;
  if (user?.role === "CUSTOMER") {
    const [activeResult, favResult] = await Promise.all([
      api.GET("/api/v1/customers/me/leads/active", { cache: "no-store" }),
      api.GET("/api/v1/customers/me/favourites", { params: { query: { page: 0, size: 200 } as never }, cache: "no-store" }),
    ]);
    const active = activeResult.data?.data;
    if (active?.id && active.tripId && active.tripTitle) {
      activeLead = { leadId: active.id, tripId: active.tripId, tripTitle: active.tripTitle };
    }
    favourited = Boolean(favResult.data?.data?.content?.some((f) => f.trip?.id === id));
  }

  let company: Awaited<ReturnType<typeof loadCompany>> = null;
  if (trip.company?.companyId) {
    company = await loadCompany(api, trip.company.companyId);
  }

  const inclusions = [
    { active: trip.visaIncluded, icon: ShieldCheck, label: t("inclusionVisa") },
    { active: trip.transportationIncluded, icon: Car, label: t("inclusionTransport") },
    { active: trip.mealsIncluded, icon: Soup, label: t("inclusionMeals") },
    { active: trip.guideIncluded, icon: UserRound, label: t("inclusionGuide") },
    { active: trip.zamzamIncluded, icon: Sparkles, label: t("inclusionZamzam") },
    { active: trip.fastTrainIncluded, icon: TrainFront, label: t("inclusionHaramainTrain") },
  ].filter((i) => i.active);

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="grid gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          <div className="relative h-56 overflow-hidden rounded-2xl sm:h-72">
            <TripImage tier={trip.tier} className="h-full w-full" />
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
            <Stat icon={CalendarDays} label={t("departs")} value={formatDate(trip.departureDate)} />
            <Stat icon={CalendarDays} label={t("returns")} value={formatDate(trip.returnDate)} />
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
              />
              <RouteLeg
                label={t("return")}
                from={trip.returnDepartureAirport}
                to={trip.returnArrivalAirport}
                airline={trip.airline}
              />
              {trip.transitCount ? (
                <p className="text-xs text-muted-foreground">
                  {t("stopsCount", { count: trip.transitCount })}
                  {trip.transitCity ? t("viaCity", { city: trip.transitCity }) : ""}
                  {trip.transitDuration ? ` (${trip.transitDuration})` : ""}
                </p>
              ) : null}
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

          {trip.hotels?.length ? (
            <div>
              <h2 className="mb-3 font-heading text-lg font-semibold">{t("hotels")}</h2>
              <div className="grid gap-4 sm:grid-cols-2">
                {trip.hotels.map((th, i) => (
                  <Card key={i}>
                    <CardContent className="space-y-1.5 pt-6">
                      <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
                        {th.city === "MAKKAH" ? t("makkah") : t("madinah")}
                      </p>
                      <p className="font-heading font-semibold">{th.hotel?.name}</p>
                      <div className="flex items-center gap-1 text-amber-500">
                        {Array.from({ length: th.hotel?.stars ?? 0 }).map((_, j) => (
                          <BadgeCheck key={j} className="size-3.5" />
                        ))}
                      </div>
                      {th.hotel?.distanceToHaramM != null && (
                        <p className="text-sm text-muted-foreground">
                          {t("distanceFrom", {
                            distance: th.hotel.distanceToHaramM,
                            place: th.city === "MAKKAH" ? t("haram") : t("prophetsMosque"),
                          })}
                          {th.hotel.canWalk ? t("walkableSuffix") : ""}
                        </p>
                      )}
                      {th.freeBusIncluded && (
                        <p className="flex items-center gap-1 text-sm text-primary">
                          <Bus className="size-3.5" /> {t("freeShuttle")}
                        </p>
                      )}
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          ) : null}

          {trip.roomPrices?.length ? (
            <div>
              <h2 className="mb-3 font-heading text-lg font-semibold">{t("roomPrices")}</h2>
              <div className="overflow-hidden rounded-xl border border-border">
                <table className="w-full text-sm">
                  <tbody>
                    {trip.roomPrices.map((p, i) => (
                      <tr key={i} className="border-b border-border last:border-0 even:bg-secondary/30">
                        <td className="p-3">{p.roomType ? (ROOM_LABEL[p.roomType] ?? p.roomType) : "—"}</td>
                        <td className="p-3 text-right font-semibold">{formatMoney(p.price, trip.currency)}</td>
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

        <div className="space-y-4 lg:sticky lg:top-24 lg:h-fit">
          <Card>
            <CardContent className="space-y-4 pt-6">
              <p className="text-sm text-muted-foreground">{t("seatsLeft", { count: trip.availableSeats ?? 0 })}</p>
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

          {company ? (
            <CompanyCard company={company} />
          ) : (
            <Card className="border-dashed">
              <CardContent className="flex flex-col items-center gap-2 py-8 text-center text-muted-foreground">
                <Lock className="size-6" />
                <p className="text-sm">{t("operatorLocked")}</p>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
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
}: {
  label: string;
  from?: { iataCode?: string; city?: string };
  to?: { iataCode?: string; city?: string };
  airline?: string;
}) {
  return (
    <div className="flex items-center justify-between text-sm">
      <span className="w-16 shrink-0 text-xs font-medium text-muted-foreground">{label}</span>
      <span className="flex-1 text-right">{from?.city ?? from?.iataCode}</span>
      <span className="mx-3 text-muted-foreground">✈ {airline}</span>
      <span className="flex-1">{to?.city ?? to?.iataCode}</span>
    </div>
  );
}

/** 403 means "not revealed to this caller yet" — a normal state, not an error to surface. */
async function loadCompany(api: Awaited<ReturnType<typeof apiClient>>, companyId: string) {
  const result = await api.GET("/api/v1/companies/{id}", { params: { path: { id: companyId } }, cache: "no-store" });
  if (result.response.status === 403) return null;
  if (result.error) throw new ApiError(result.error as never);
  return result.data?.data ?? null;
}
