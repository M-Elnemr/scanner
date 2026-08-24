import Link from "next/link";
import type { Metadata } from "next";
import { ArrowUp, ArrowDown, ArrowUpDown } from "lucide-react";
import { getLocale, getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { listApprovedCompanies, listTrips } from "@/lib/admin/reference-data";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";
import { AdminLeadStatusBadge } from "@/components/admin/admin-lead-status-badge";
import { AdminFilterBar } from "@/components/admin/admin-filter-bar";
import { TablePagination } from "@/components/admin/table-pagination";
import { WhatsAppButtons } from "@/components/admin/whatsapp-buttons";
import { PullToRefresh } from "@/components/admin/pull-to-refresh";
import { formatDate } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";

type SortField = "status" | "createdAt";

function SortableHead({
  field,
  label,
  sortBy,
  sortDir,
  baseParams,
}: {
  field: SortField;
  label: string;
  sortBy: SortField;
  sortDir: "asc" | "desc";
  baseParams: URLSearchParams;
}) {
  const isActive = sortBy === field;
  const nextDir = isActive && sortDir === "asc" ? "desc" : "asc";
  const params = new URLSearchParams(baseParams);
  params.set("sortBy", field);
  params.set("sortDir", nextDir);
  const Icon = isActive ? (sortDir === "asc" ? ArrowUp : ArrowDown) : ArrowUpDown;

  return (
    <TableHead>
      <Link href={`?${params.toString()}`} className="inline-flex items-center gap-1 hover:text-foreground">
        {label}
        <Icon className={`size-3.5 ${isActive ? "" : "text-muted-foreground/50"}`} />
      </Link>
    </TableHead>
  );
}

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("leads") };
}

export default async function AdminLeadsPage(props: PageProps<"/admin/leads">) {
  const searchParams = await props.searchParams;
  const status = typeof searchParams.status === "string" ? searchParams.status : undefined;
  const search = typeof searchParams.search === "string" ? searchParams.search : undefined;
  const companyId = typeof searchParams.companyId === "string" ? searchParams.companyId : undefined;
  const tripId = typeof searchParams.tripId === "string" ? searchParams.tripId : undefined;
  const page = typeof searchParams.page === "string" ? Number(searchParams.page) : 0;
  const sortBy: SortField = searchParams.sortBy === "createdAt" ? "createdAt" : "status";
  const sortDir: "asc" | "desc" = searchParams.sortDir === "desc" ? "desc" : "asc";

  // Preserves every filter/sort param except pagination when a header link or filter is followed.
  const baseParams = new URLSearchParams();
  for (const [key, value] of Object.entries(searchParams)) {
    if (key === "page" || typeof value !== "string") continue;
    baseParams.set(key, value);
  }

  const t = await getTranslations("admin.leads");
  const tStatus = await getTranslations("admin.leadStatus");
  const locale = (await getLocale()) as "ar" | "en";
  const STATUS_OPTIONS = (
    [
      "INTERESTED",
      "CONTACTED",
      "CONFIRMED",
      "PENDING_DEPOSIT_CONFIRMATION",
      "DEPOSIT_PAID",
      "PENDING_FULL_PAYMENT_CONFIRMATION",
      "FULLY_PAID",
      "PENDING_COMMISSION_CONFIRMATION",
      "COMMISSION_PAID",
      "CASHBACK_PAID",
      "CANCELLED",
    ] as const
  ).map((value) => ({ value, label: tStatus(value) }));

  const api = await apiClient();
  const [result, companies, trips] = await Promise.all([
    api.GET("/api/v1/admin/leads", {
      params: {
        query: {
          ...(status
            ? {
                status: status as
                  | "INTERESTED"
                  | "CONTACTED"
                  | "CONFIRMED"
                  | "PENDING_DEPOSIT_CONFIRMATION"
                  | "DEPOSIT_PAID"
                  | "PENDING_FULL_PAYMENT_CONFIRMATION"
                  | "FULLY_PAID"
                  | "PENDING_COMMISSION_CONFIRMATION"
                  | "COMMISSION_PAID"
                  | "CASHBACK_PAID"
                  | "CANCELLED",
              }
            : {}),
          ...(search ? { search } : {}),
          ...(companyId ? { companyId } : {}),
          ...(tripId ? { tripId } : {}),
          ...pageableQuery(page, 20, `${sortBy},${sortDir}`),
        },
      },
      cache: "no-store",
    }),
    listApprovedCompanies(),
    listTrips(companyId),
  ]);
  const pageData = result.data?.data;
  const leads = pageData?.content ?? [];

  return (
    <PullToRefresh>
      <div className="space-y-6">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">{t("title")}</h1>
          <p className="text-sm text-muted-foreground">{t("total", { count: pageData?.totalElements ?? 0 })}</p>
        </div>

        <AdminFilterBar
          statusOptions={STATUS_OPTIONS}
          searchPlaceholder={t("searchPlaceholder")}
          companyOptions={companies.map((c) => ({ value: c.id, label: c.name }))}
          tripOptions={trips.map((trip) => ({ value: trip.id, label: `${trip.title} · ${trip.tripCode}` }))}
        />

        <div className="overflow-hidden rounded-xl border border-border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("colCustomer")}</TableHead>
                <TableHead>{t("colTrip")}</TableHead>
                <TableHead>{t("colCompany")}</TableHead>
                <TableHead>{t("colCommission")}</TableHead>
                <SortableHead field="status" label={t("colStatus")} sortBy={sortBy} sortDir={sortDir} baseParams={baseParams} />
                <SortableHead field="createdAt" label={t("colCreated")} sortBy={sortBy} sortDir={sortDir} baseParams={baseParams} />
                <TableHead>{t("colWhatsapp")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {leads.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} className="py-10 text-center text-muted-foreground">
                    {t("empty")}
                  </TableCell>
                </TableRow>
              )}
              {leads.map((l) => (
                <TableRow key={l.id}>
                  <TableCell className="font-medium">
                    <Link href={`/admin/leads/${l.id}`} className="hover:text-primary">
                      {l.customer?.fullName ?? "—"}
                    </Link>
                    <p className="text-xs text-muted-foreground">{l.customer?.phone}</p>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{l.tripTitle}</TableCell>
                  <TableCell className="text-muted-foreground">{l.companyName}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {l.commissionAmount != null ? formatMoney(l.commissionAmount, { code: "EGP" }, locale) : "—"}
                  </TableCell>
                  <TableCell>
                    <AdminLeadStatusBadge status={l.status} />
                  </TableCell>
                  <TableCell className="text-muted-foreground">{formatDate(l.createdAt, undefined, locale)}</TableCell>
                  <TableCell>
                    <p className="text-xs text-muted-foreground">{l.customer?.phone}</p>
                    {l.id && (
                      <WhatsAppButtons
                        leadId={l.id}
                        compact
                        canMarkContacted={(l.availableActions ?? []).includes("MARK_CONTACTED")}
                        canConfirmViaCompany={(l.availableActions ?? []).includes("CONFIRM_VIA_COMPANY")}
                      />
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        <TablePagination page={pageData?.page ?? 0} totalPages={pageData?.totalPages ?? 1} />
      </div>
    </PullToRefresh>
  );
}
