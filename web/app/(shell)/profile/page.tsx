import type { Metadata } from "next";
import { apiClient, requireUser } from "@/lib/auth/server";
import { ProfileForm } from "@/components/profile/profile-form";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = { title: "My profile" };

export default async function ProfilePage() {
  const user = await requireUser();

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center text-muted-foreground">
        Profile settings are available to customer accounts.
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/customers/me", { cache: "no-store" });
  const customer = result.data?.data;

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="mb-8 font-heading text-3xl font-bold tracking-tight">My profile</h1>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-lg">Account details</CardTitle>
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
