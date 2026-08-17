import type { Metadata } from "next";
import { listCities } from "@/lib/admin/cities";
import { CompanyForm } from "@/components/admin/company-form";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = { title: "New company · Admin" };

export default async function NewCompanyPage() {
  const cities = await listCities();

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="font-heading text-2xl font-bold tracking-tight">New company</h1>
      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-lg">Company details</CardTitle>
        </CardHeader>
        <CardContent>
          <CompanyForm mode="create" cities={cities} initial={{}} />
        </CardContent>
      </Card>
    </div>
  );
}
