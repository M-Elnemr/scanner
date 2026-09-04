import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";

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

// No date-range picker (yet) — every admin analytics endpoint already defaults to the last 30
// days and caps at 90 server-side (see AnalyticsReportingService), so this page just renders
// that default window. Client-side hits Firebase/GA-style consoles nowhere; this reads straight
// from the app's own analytics_events pipeline, kept cheap by AnalyticsReportingService's bounded,
// indexed queries.
export default async function AdminAnalyticsPage() {
  const t = await getTranslations("admin.analytics");

  const api = await apiClient();
  const [eventsResult, audienceResult, mostViewedResult] = await Promise.all([
    api.GET("/api/v1/admin/analytics/events-summary", { cache: "no-store" }),
    api.GET("/api/v1/admin/analytics/audience", { cache: "no-store" }),
    api.GET("/api/v1/admin/analytics/most-viewed-trips", { params: { query: { limit: 10 } }, cache: "no-store" }),
  ]);

  const eventsByType = eventsResult.data?.data ?? [];
  const audience = audienceResult.data?.data;
  const mostViewedTrips = mostViewedResult.data?.data ?? [];
  const totalEvents = (audience?.guestEvents ?? 0) + (audience?.identifiedEvents ?? 0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold tracking-tight">{t("title")}</h1>
        <p className="text-sm text-muted-foreground">
          {t("subtitle")} · {t("rangeLast30Days")}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard label={t("totalEvents")} value={totalEvents} />
        <StatCard label={t("guestEvents")} value={audience?.guestEvents ?? 0} />
        <StatCard label={t("identifiedEvents")} value={audience?.identifiedEvents ?? 0} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
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
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("colTrip")}</TableHead>
                <TableHead className="text-end">{t("colViews")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {mostViewedTrips.length === 0 && (
                <TableRow>
                  <TableCell colSpan={2} className="py-8 text-center text-muted-foreground">
                    {t("empty")}
                  </TableCell>
                </TableRow>
              )}
              {mostViewedTrips.map((row) => (
                <TableRow key={row.tripId}>
                  <TableCell className="font-medium">{row.tripTitle ?? row.tripId}</TableCell>
                  <TableCell className="text-end text-muted-foreground">
                    {(row.count ?? 0).toLocaleString()}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
    </div>
  );
}
