import Link from "next/link";
import type { Metadata } from "next";
import { requireUser, apiClient } from "@/lib/auth/server";
import { formatDate, tripDurationNights } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";
import { Button } from "@/components/ui/button";
import type { components } from "@/lib/api/schema";

export const metadata: Metadata = { title: "Compare trips" };

type TripSummary = components["schemas"]["TripSummaryResponse"];

const ROWS: { label: string; render: (trip: TripSummary) => React.ReactNode }[] = [
  { label: "Tier", render: (t) => t.tier },
  { label: "Departure", render: (t) => formatDate(t.departureDate) },
  { label: "Nights", render: (t) => tripDurationNights(t.departureDate, t.returnDate) ?? "—" },
  { label: "Airline", render: (t) => t.airline },
  { label: "Route", render: (t) => `${t.outboundDepartureAirport?.iataCode ?? "—"} → ${t.outboundArrivalAirport?.iataCode ?? "—"}` },
  { label: "Makkah nights", render: (t) => t.daysInMakkah },
  { label: "Madinah nights", render: (t) => t.daysInMadinah },
  { label: "Seats left", render: (t) => t.availableSeats },
  { label: "Price from", render: (t) => formatMoney(t.priceStartsFrom, t.currency) },
];

export default async function ComparePage(props: PageProps<"/trips/compare">) {
  const user = await requireUser();
  const sp = await props.searchParams;
  const ids = (Array.isArray(sp.ids) ? sp.ids[0] : sp.ids)?.split(",").filter(Boolean) ?? [];

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center">
        <p className="text-muted-foreground">Comparing trips is available to customer accounts.</p>
      </div>
    );
  }

  if (ids.length < 2) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center">
        <p className="text-muted-foreground">Pick at least two trips from the browse page to compare them.</p>
        <Button className="mt-4" render={<Link href="/trips" />}>
          Browse trips
        </Button>
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/trips/compare", { params: { query: { ids } } });
  const trips = result.data?.data ?? [];

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="mb-8 font-heading text-3xl font-bold tracking-tight">Compare trips</h1>

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
