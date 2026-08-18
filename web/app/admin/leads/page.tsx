import Link from "next/link";
import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";
import { AdminLeadStatusBadge } from "@/components/admin/admin-lead-status-badge";
import { AdminFilterBar } from "@/components/admin/admin-filter-bar";
import { TablePagination } from "@/components/admin/table-pagination";
import { WhatsAppButtons } from "@/components/admin/whatsapp-buttons";
import { formatDate } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("leads") };
}

export default async function AdminLeadsPage(props: PageProps<"/admin/leads">) {
  const searchParams = await props.searchParams;
  const status = typeof searchParams.status === "string" ? searchParams.status : undefined;
  const search = typeof searchParams.search === "string" ? searchParams.search : undefined;
  const page = typeof searchParams.page === "string" ? Number(searchParams.page) : 0;

  const t = await getTranslations("admin.leads");
  const tStatus = await getTranslations("admin.leadStatus");
  const STATUS_OPTIONS = (
    [
      "INTERESTED",
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
  const result = await api.GET("/api/v1/admin/leads", {
    params: {
      query: {
        ...(status
          ? {
              status: status as
                | "INTERESTED"
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
        ...pageableQuery(page, 20, "createdAt,desc"),
      },
    },
    cache: "no-store",
  });
  const pageData = result.data?.data;
  const leads = pageData?.content ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold tracking-tight">{t("title")}</h1>
        <p className="text-sm text-muted-foreground">{t("total", { count: pageData?.totalElements ?? 0 })}</p>
      </div>

      <AdminFilterBar statusOptions={STATUS_OPTIONS} searchPlaceholder={t("searchPlaceholder")} />

      <div className="overflow-hidden rounded-xl border border-border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("colCustomer")}</TableHead>
              <TableHead>{t("colTrip")}</TableHead>
              <TableHead>{t("colCompany")}</TableHead>
              <TableHead>{t("colCommission")}</TableHead>
              <TableHead>{t("colStatus")}</TableHead>
              <TableHead>{t("colCreated")}</TableHead>
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
                  {l.commissionAmount != null ? formatMoney(l.commissionAmount, { code: "EGP" }) : "—"}
                </TableCell>
                <TableCell>
                  <AdminLeadStatusBadge status={l.status} />
                </TableCell>
                <TableCell className="text-muted-foreground">{formatDate(l.createdAt)}</TableCell>
                <TableCell>{l.id && <WhatsAppButtons leadId={l.id} compact />}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <TablePagination page={pageData?.page ?? 0} totalPages={pageData?.totalPages ?? 1} />
    </div>
  );
}
