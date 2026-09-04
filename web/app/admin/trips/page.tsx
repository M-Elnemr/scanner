import Link from "next/link";
import type { Metadata } from "next";
import { Plus } from "lucide-react";
import { getLocale, getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { listApprovedCompanies, listAirports } from "@/lib/admin/reference-data";
import { listCities } from "@/lib/admin/cities";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";
import { TripStatusBadge } from "@/components/admin/trip-status-badge";
import { TripRowActions } from "@/components/admin/trip-row-actions";
import { CopyTripDetailsButton } from "@/components/admin/copy-trip-details-button";
import { AdminFilterBar } from "@/components/admin/admin-filter-bar";
import { AdvancedFilterSheet } from "@/components/trip/advanced-filter-sheet";
import { TablePagination } from "@/components/admin/table-pagination";
import { formatDate } from "@/lib/format/date";

function firstParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("trips") };
}

export default async function AdminTripsPage(props: PageProps<"/admin/trips">) {
  const searchParams = await props.searchParams;
  const status = typeof searchParams.status === "string" ? searchParams.status : undefined;
  const search = typeof searchParams.search === "string" ? searchParams.search : undefined;
  const companyId = typeof searchParams.companyId === "string" ? searchParams.companyId : undefined;
  const page = typeof searchParams.page === "string" ? Number(searchParams.page) : 0;

  // Filter parity with the public /trips browse — see AdvancedFilterSheet, shared with the
  // public page so the two can't drift on what these params mean.
  const roomSize = firstParam(searchParams.roomSize) ? Number(firstParam(searchParams.roomSize)) : undefined;
  const minPrice = firstParam(searchParams.minPrice) ? Number(firstParam(searchParams.minPrice)) : undefined;
  const maxPrice = firstParam(searchParams.maxPrice) ? Number(firstParam(searchParams.maxPrice)) : undefined;
  const minDays = firstParam(searchParams.minDays) ? Number(firstParam(searchParams.minDays)) : undefined;
  const maxDays = firstParam(searchParams.maxDays) ? Number(firstParam(searchParams.maxDays)) : undefined;
  const departureFrom = firstParam(searchParams.departureFrom);
  const departureTo = firstParam(searchParams.departureTo);
  const cityId = firstParam(searchParams.cityId);
  const departureAirportId = firstParam(searchParams.departureAirportId);

  const t = await getTranslations("admin.trips");
  const tStatus = await getTranslations("admin.tripStatus");
  const locale = (await getLocale()) as "ar" | "en";
  const STATUS_OPTIONS = (["DRAFT", "PUBLISHED", "CLOSED", "EXPIRED"] as const).map((value) => ({
    value,
    label: tStatus(value),
  }));

  const api = await apiClient();
  const [result, companies, cities, airports] = await Promise.all([
    api.GET("/api/v1/admin/trips", {
      params: {
        query: {
          ...(status ? { status: status as "DRAFT" | "PUBLISHED" | "CLOSED" | "EXPIRED" } : {}),
          ...(search ? { search } : {}),
          ...(companyId ? { companyId } : {}),
          roomSize,
          minPrice,
          maxPrice,
          minDays,
          maxDays,
          departureFrom,
          departureTo,
          cityId,
          departureAirportId,
          ...pageableQuery(page, 20, "departureDate,desc"),
        },
      },
      cache: "no-store",
    }),
    listApprovedCompanies(),
    listCities(),
    listAirports(),
  ]);
  const pageData = result.data?.data;
  const trips = pageData?.content ?? [];
  // Same carve-out as the public /trips page: only the Egyptian leg is ever a departure airport.
  const departureAirports = airports.filter((a) => a.countryIso2 === "EG");

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">{t("title")}</h1>
          <p className="text-sm text-muted-foreground">{t("total", { count: pageData?.totalElements ?? 0 })}</p>
        </div>
        <Button render={<Link href="/admin/trips/new" />}>
          <Plus /> {t("new")}
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <AdminFilterBar
          statusOptions={STATUS_OPTIONS}
          searchPlaceholder={t("searchPlaceholder")}
          companyOptions={companies.map((c) => ({ value: c.id, label: c.name }))}
        />
        <AdvancedFilterSheet pathname="/admin/trips" cities={cities} airports={departureAirports} />
      </div>

      <div className="overflow-hidden rounded-xl border border-border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("colTrip")}</TableHead>
              <TableHead>{t("colCompany")}</TableHead>
              <TableHead>{t("colTier")}</TableHead>
              <TableHead>{t("colDeparts")}</TableHead>
              <TableHead>{t("colSeats")}</TableHead>
              <TableHead>{t("colStatus")}</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {trips.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="py-10 text-center text-muted-foreground">
                  {t("empty")}
                </TableCell>
              </TableRow>
            )}
            {trips.map((t) => (
              <TableRow key={t.id}>
                <TableCell className="font-medium">
                  <Link href={`/admin/trips/${t.id}`} className="hover:text-primary">
                    {t.title}
                  </Link>
                  <p className="text-xs text-muted-foreground">{t.tripCode}</p>
                </TableCell>
                <TableCell className="text-muted-foreground">{t.companyName}</TableCell>
                <TableCell>
                  <Badge variant="outline">{t.tier}</Badge>
                </TableCell>
                <TableCell className="text-muted-foreground">{formatDate(t.departureDate, undefined, locale)}</TableCell>
                <TableCell className="text-muted-foreground">{t.availableSeats ?? "—"}</TableCell>
                <TableCell>
                  <TripStatusBadge status={t.status} />
                </TableCell>
                <TableCell>
                  <div className="flex items-center justify-end gap-1">
                    {t.id && <CopyTripDetailsButton tripId={t.id} />}
                    <TripRowActions id={t.id ?? ""} status={t.status} companies={companies} />
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <TablePagination page={pageData?.page ?? 0} totalPages={pageData?.totalPages ?? 1} />
    </div>
  );
}
