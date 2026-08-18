import Link from "next/link";
import type { Metadata } from "next";
import { Plus } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { Button } from "@/components/ui/button";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";
import { CompanyStatusBadge } from "@/components/admin/company-status-badge";
import { CompanyRowActions } from "@/components/admin/company-row-actions";
import { AdminFilterBar } from "@/components/admin/admin-filter-bar";
import { TablePagination } from "@/components/admin/table-pagination";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("companies") };
}

export default async function AdminCompaniesPage(props: PageProps<"/admin/companies">) {
  const searchParams = await props.searchParams;
  const status = typeof searchParams.status === "string" ? searchParams.status : undefined;
  const search = typeof searchParams.search === "string" ? searchParams.search : undefined;
  const page = typeof searchParams.page === "string" ? Number(searchParams.page) : 0;

  const t = await getTranslations("admin.companies");
  const tStatus = await getTranslations("admin.companyStatus");
  const STATUS_OPTIONS = [
    { value: "PENDING", label: tStatus("PENDING") },
    { value: "APPROVED", label: tStatus("APPROVED") },
    { value: "REJECTED", label: tStatus("REJECTED") },
    { value: "SUSPENDED", label: tStatus("SUSPENDED") },
  ];

  const api = await apiClient();
  const result = await api.GET("/api/v1/admin/companies", {
    params: {
      query: {
        ...(status ? { status: status as "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED" } : {}),
        ...(search ? { search } : {}),
        ...pageableQuery(page, 20, "createdAt,desc"),
      },
    },
    cache: "no-store",
  });
  const pageData = result.data?.data;
  const companies = pageData?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">{t("title")}</h1>
          <p className="text-sm text-muted-foreground">{t("total", { count: pageData?.totalElements ?? 0 })}</p>
        </div>
        <Button render={<Link href="/admin/companies/new" />}>
          <Plus /> {t("new")}
        </Button>
      </div>

      <AdminFilterBar statusOptions={STATUS_OPTIONS} searchPlaceholder={t("searchPlaceholder")} />

      <div className="overflow-hidden rounded-xl border border-border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("colCompany")}</TableHead>
              <TableHead>{t("colLicence")}</TableHead>
              <TableHead>{t("colStatus")}</TableHead>
              <TableHead>{t("colCommission")}</TableHead>
              <TableHead>{t("colRating")}</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {companies.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-10 text-center text-muted-foreground">
                  {t("empty")}
                </TableCell>
              </TableRow>
            )}
            {companies.map((c) => (
              <TableRow key={c.id}>
                <TableCell className="font-medium">
                  <Link href={`/admin/companies/${c.id}`} className="hover:text-primary">
                    {c.companyName}
                  </Link>
                </TableCell>
                <TableCell className="text-muted-foreground">{c.licenseNumber}</TableCell>
                <TableCell>
                  <CompanyStatusBadge status={c.status} />
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {c.commissionPerTraveler != null ? t("commissionValue", { amount: c.commissionPerTraveler }) : "—"}
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {c.ratingAvg ? t("ratingValue", { rating: c.ratingAvg.toFixed(1), count: c.ratingCount ?? 0 }) : "—"}
                </TableCell>
                <TableCell>
                  <CompanyRowActions id={c.id ?? ""} status={c.status} commissionPerTraveler={c.commissionPerTraveler} />
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
