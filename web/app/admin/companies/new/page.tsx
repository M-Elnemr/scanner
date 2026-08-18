import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { listCities } from "@/lib/admin/cities";
import { CompanyForm } from "@/components/admin/company-form";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("companyNew") };
}

export default async function NewCompanyPage() {
  const t = await getTranslations("admin.companies");
  const cities = await listCities();

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="font-heading text-2xl font-bold tracking-tight">{t("new")}</h1>
      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-lg">{t("detailsTitle")}</CardTitle>
        </CardHeader>
        <CardContent>
          <CompanyForm mode="create" cities={cities} initial={{}} />
        </CardContent>
      </Card>
    </div>
  );
}
