import Link from "next/link";
import type { Metadata } from "next";
import { Plus } from "lucide-react";
import { apiClient } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { listApprovedCompanies } from "@/lib/admin/reference-data";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";
import { TripStatusBadge } from "@/components/admin/trip-status-badge";
import { TripRowActions } from "@/components/admin/trip-row-actions";
import { AdminFilterBar } from "@/components/admin/admin-filter-bar";
import { TablePagination } from "@/components/admin/table-pagination";
import { formatDate } from "@/lib/format/date";

export const metadata: Metadata = { title: "Trips · Admin" };

const STATUS_OPTIONS = [
  { value: "DRAFT", label: "Draft" },
  { value: "PUBLISHED", label: "Published" },
  { value: "CLOSED", label: "Closed" },
  { value: "EXPIRED", label: "Expired" },
];

export default async function AdminTripsPage(props: PageProps<"/admin/trips">) {
  const searchParams = await props.searchParams;
  const status = typeof searchParams.status === "string" ? searchParams.status : undefined;
  const search = typeof searchParams.search === "string" ? searchParams.search : undefined;
  const page = typeof searchParams.page === "string" ? Number(searchParams.page) : 0;

  const api = await apiClient();
  const [result, companies] = await Promise.all([
    api.GET("/api/v1/admin/trips", {
      params: {
        query: {
          ...(status ? { status: status as "DRAFT" | "PUBLISHED" | "CLOSED" | "EXPIRED" } : {}),
          ...(search ? { search } : {}),
          ...pageableQuery(page, 20, "departureDate,desc"),
        },
      },
      cache: "no-store",
    }),
    listApprovedCompanies(),
  ]);
  const pageData = result.data?.data;
  const trips = pageData?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">Trips</h1>
          <p className="text-sm text-muted-foreground">{pageData?.totalElements ?? 0} total</p>
        </div>
        <Button render={<Link href="/admin/trips/new" />}>
          <Plus /> New trip
        </Button>
      </div>

      <AdminFilterBar statusOptions={STATUS_OPTIONS} searchPlaceholder="Search by title or trip code…" />

      <div className="overflow-hidden rounded-xl border border-border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Trip</TableHead>
              <TableHead>Company</TableHead>
              <TableHead>Tier</TableHead>
              <TableHead>Departs</TableHead>
              <TableHead>Seats</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {trips.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="py-10 text-center text-muted-foreground">
                  No trips match these filters.
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
                <TableCell className="text-muted-foreground">{formatDate(t.departureDate)}</TableCell>
                <TableCell className="text-muted-foreground">{t.availableSeats ?? "—"}</TableCell>
                <TableCell>
                  <TripStatusBadge status={t.status} />
                </TableCell>
                <TableCell>
                  <TripRowActions id={t.id ?? ""} status={t.status} companies={companies} />
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
