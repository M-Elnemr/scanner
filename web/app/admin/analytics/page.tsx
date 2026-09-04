import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";
import { AnalyticsRangePicker } from "@/components/admin/analytics-range-picker";
import { EventsOverTimeChart } from "@/components/admin/events-over-time-chart";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("analytics") };
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-border bg-background p-4">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-1 font-heading text-2xl font-bold tracking-tight">{value.toLocaleString()}</p>
    </div>
  );
}

function TripCountTable({
  rows,
  colTrip,
  colCount,
  emptyLabel,
}: {
  rows: { tripId?: string; tripTitle?: string | null; count?: number }[];
  colTrip: string;
  colCount: string;
  emptyLabel: string;
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{colTrip}</TableHead>
          <TableHead className="text-end">{colCount}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.length === 0 && (
          <TableRow>
            <TableCell colSpan={2} className="py-8 text-center text-muted-foreground">
              {emptyLabel}
            </TableCell>
          </TableRow>
        )}
        {rows.map((row) => (
          <TableRow key={row.tripId}>
            <TableCell className="font-medium">{row.tripTitle ?? row.tripId}</TableCell>
            <TableCell className="text-end text-muted-foreground">{(row.count ?? 0).toLocaleString()}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export default async function AdminAnalyticsPage(props: PageProps<"/admin/analytics">) {
  const t = await getTranslations("admin.analytics");
  const searchParams = await props.searchParams;
  const from = first(searchParams.from);
  const to = first(searchParams.to);
  const range = { from, to };

  const api = await apiClient();
  const [eventsResult, audienceResult, mostViewedResult, mostSharedResult, overTimeResult] = await Promise.all([
    api.GET("/api/v1/admin/analytics/events-summary", { params: { query: range }, cache: "no-store" }),
    api.GET("/api/v1/admin/analytics/audience", { params: { query: range }, cache: "no-store" }),
    api.GET("/api/v1/admin/analytics/most-viewed-trips", {
      params: { query: { ...range, limit: 10 } },
      cache: "no-store",
    }),
    api.GET("/api/v1/admin/analytics/most-shared-trips", {
      params: { query: { ...range, limit: 10 } },
      cache: "no-store",
    }),
    api.GET("/api/v1/admin/analytics/events-over-time", { params: { query: range }, cache: "no-store" }),
  ]);

  const eventsByType = eventsResult.data?.data ?? [];
  const audience = audienceResult.data?.data;
  const mostViewedTrips = mostViewedResult.data?.data ?? [];
  const mostSharedTrips = mostSharedResult.data?.data ?? [];
  const overTime = (overTimeResult.data?.data ?? []).map((p) => ({ bucket: p.bucket ?? "", count: p.count ?? 0 }));
  const totalEvents = (audience?.guestEvents ?? 0) + (audience?.identifiedEvents ?? 0);

  const rangeLabel = from && to ? `${new Date(from).toLocaleString()} – ${new Date(to).toLocaleString()}` : t("rangeLast30Days");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold tracking-tight">{t("title")}</h1>
        <p className="text-sm text-muted-foreground">
          {t("subtitle")} · {rangeLabel}
        </p>
      </div>

      <AnalyticsRangePicker />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label={t("totalEvents")} value={totalEvents} />
        <StatCard label={t("guestEvents")} value={audience?.guestEvents ?? 0} />
        <StatCard label={t("identifiedEvents")} value={audience?.identifiedEvents ?? 0} />
        <StatCard label={t("uniqueSignedInUsers")} value={audience?.uniqueSignedInUsers ?? 0} />
      </div>

      <div className="overflow-hidden rounded-xl border border-border bg-background">
        <div className="border-b border-border px-4 py-3">
          <h2 className="font-medium">{t("eventsOverTime")}</h2>
        </div>
        <EventsOverTimeChart data={overTime} emptyLabel={t("empty")} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="overflow-hidden rounded-xl border border-border bg-background">
          <div className="border-b border-border px-4 py-3">
            <h2 className="font-medium">{t("eventsByType")}</h2>
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("colEventType")}</TableHead>
                <TableHead className="text-end">{t("colCount")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {eventsByType.length === 0 && (
                <TableRow>
                  <TableCell colSpan={2} className="py-8 text-center text-muted-foreground">
                    {t("empty")}
                  </TableCell>
                </TableRow>
              )}
              {eventsByType.map((row) => (
                <TableRow key={row.eventType}>
                  <TableCell className="font-medium">{row.eventType}</TableCell>
                  <TableCell className="text-end text-muted-foreground">
                    {(row.count ?? 0).toLocaleString()}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        <div className="overflow-hidden rounded-xl border border-border bg-background">
          <div className="border-b border-border px-4 py-3">
            <h2 className="font-medium">{t("mostViewedTrips")}</h2>
          </div>
          <TripCountTable
            rows={mostViewedTrips}
            colTrip={t("colTrip")}
            colCount={t("colViews")}
            emptyLabel={t("empty")}
          />
        </div>

        <div className="overflow-hidden rounded-xl border border-border bg-background">
          <div className="border-b border-border px-4 py-3">
            <h2 className="font-medium">{t("mostSharedTrips")}</h2>
          </div>
          <TripCountTable
            rows={mostSharedTrips}
            colTrip={t("colTrip")}
            colCount={t("colShares")}
            emptyLabel={t("empty")}
          />
        </div>
      </div>
    </div>
  );
}
