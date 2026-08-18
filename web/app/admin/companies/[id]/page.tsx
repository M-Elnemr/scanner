import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { apiClient } from "@/lib/auth/server";
import { listCities } from "@/lib/admin/cities";
import { CompanyForm } from "@/components/admin/company-form";
import { CompanyStatusBadge } from "@/components/admin/company-status-badge";
import { CompanyRowActions } from "@/components/admin/company-row-actions";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("companyEdit") };
}

export default async function EditCompanyPage(props: PageProps<"/admin/companies/[id]">) {
  const { id } = await props.params;
  const t = await getTranslations("admin.companies");
  const api = await apiClient();
  const [companyResult, cities] = await Promise.all([
    api.GET("/api/v1/admin/companies/{id}", { params: { path: { id } }, cache: "no-store" }),
    listCities(),
  ]);
  const company = companyResult.data?.data;
  if (!company?.id) notFound();

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">{company.companyName}</h1>
          <div className="mt-1 flex items-center gap-2">
            <CompanyStatusBadge status={company.status} />
            {company.rejectionReason && (
              <span className="text-xs text-muted-foreground">{t("reasonLabel", { reason: company.rejectionReason })}</span>
            )}
          </div>
        </div>
        <CompanyRowActions id={company.id} status={company.status} commissionPerTraveler={company.commissionPerTraveler} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-lg">{t("detailsTitle")}</CardTitle>
        </CardHeader>
        <CardContent>
          <CompanyForm
            mode="edit"
            companyId={company.id}
            cities={cities}
            initial={{
              companyName: company.companyName ?? "",
              licenseNumber: company.licenseNumber ?? "",
              logoUrl: company.logoUrl ?? "",
              whatsapp: company.whatsapp ?? "",
              description: company.description ?? "",
              addresses: (company.addresses ?? []).map((a) => ({
                cityId: a.cityId ?? "",
                addressText: a.addressText ?? "",
                mobileNumber: a.mobileNumber ?? "",
              })),
            }}
          />
        </CardContent>
      </Card>
    </div>
  );
}
