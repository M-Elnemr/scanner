import Link from "next/link";
import type { Metadata } from "next";
import { getLocale, getTranslations } from "next-intl/server";
import { requireUser, apiClient } from "@/lib/auth/server";
import { formatDate, tripDurationDays } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";
import { Button } from "@/components/ui/button";
import type { components } from "@/lib/api/schema";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("compare");
  return { title: t("title") };
}

type TripSummary = components["schemas"]["TripSummaryResponse"];

function buildRows(
  t: Awaited<ReturnType<typeof getTranslations>>,
  locale: "ar" | "en",
): {
  label: string;
  render: (trip: TripSummary) => React.ReactNode;
}[] {
  return [
    { label: t("rowTier"), render: (trip) => trip.tier },
    { label: t("rowDeparture"), render: (trip) => formatDate(trip.departureDate, undefined, locale) },
    { label: t("rowDuration"), render: (trip) => tripDurationDays(trip.departureDate, trip.returnDate) ?? "—" },
    { label: t("rowAirline"), render: (trip) => trip.airline },
    {
      label: t("rowRoute"),
      render: (trip) => `${trip.outboundDepartureAirport?.iataCode ?? "—"} → ${trip.outboundArrivalAirport?.iataCode ?? "—"}`,
    },
    { label: t("rowMakkahNights"), render: (trip) => trip.daysInMakkah },
    { label: t("rowMadinahNights"), render: (trip) => trip.daysInMadinah },
    { label: t("rowPriceFrom"), render: (trip) => formatMoney(trip.priceStartsFrom, trip.currency, locale) },
  ];
}

export default async function ComparePage(props: PageProps<"/trips/compare">) {
  const user = await requireUser();
  const t = await getTranslations("compare");
  const tNav = await getTranslations("nav");
  const locale = (await getLocale()) as "ar" | "en";
  const sp = await props.searchParams;
  const ids = (Array.isArray(sp.ids) ? sp.ids[0] : sp.ids)?.split(",").filter(Boolean) ?? [];

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center">
        <p className="text-muted-foreground">{t("customersOnly")}</p>
      </div>
    );
  }

  if (ids.length < 2) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center">
        <p className="text-muted-foreground">{t("pickAtLeastTwo")}</p>
        <Button className="mt-4" render={<Link href="/trips" />}>
          {tNav("browseTrips")}
        </Button>
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/trips/compare", { params: { query: { ids } } });
  const trips = result.data?.data ?? [];
  const ROWS = buildRows(t, locale);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="mb-8 font-heading text-3xl font-bold tracking-tight">{t("title")}</h1>

      <div className="overflow-x-auto rounded-2xl border border-border">
        <table className="w-full min-w-[640px] border-collapse text-sm">
          <thead>
            <tr className="border-b border-border bg-secondary/50">
              <th className="w-40 p-4 text-left font-medium text-muted-foreground">&nbsp;</th>
              {trips.map((trip) => (
                <th key={trip.id} className="p-4 text-left">
                  <Link href={`/trips/${trip.id}`} className="font-heading font-semibold hover:text-primary">
                    {trip.title}
                  </Link>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {ROWS.map((row) => (
              <tr key={row.label} className="border-b border-border last:border-0 even:bg-secondary/20">
                <td className="p-4 font-medium text-muted-foreground">{row.label}</td>
                {trips.map((trip) => (
                  <td key={trip.id} className="p-4">
                    {row.render(trip)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
