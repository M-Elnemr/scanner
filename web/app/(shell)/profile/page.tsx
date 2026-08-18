import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { apiClient, requireUser } from "@/lib/auth/server";
import { ProfileForm } from "@/components/profile/profile-form";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("profile");
  return { title: t("title") };
}

export default async function ProfilePage() {
  const user = await requireUser();
  const t = await getTranslations("profile");

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center text-muted-foreground">
        {t("customersOnly")}
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/customers/me", { cache: "no-store" });
  const customer = result.data?.data;

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="mb-8 font-heading text-3xl font-bold tracking-tight">{t("title")}</h1>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-lg">{t("accountDetails")}</CardTitle>
        </CardHeader>
        <CardContent>
          <ProfileForm
            initial={{
              fullName: customer?.fullName ?? "",
              phone: customer?.phone ?? "",
              cashbackWalletNumber: customer?.cashbackWalletNumber ?? "",
              walletType: customer?.walletType ?? "VODAFONE_CASH",
            }}
          />
        </CardContent>
      </Card>
    </div>
  );
}
