import { Star, MapPin, Phone, BadgeCheck } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { components } from "@/lib/api/schema";

type PublicCompany = components["schemas"]["PublicCompany"];

export async function CompanyCard({ company }: { company: PublicCompany }) {
  const t = await getTranslations("companyCard");

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 font-heading text-lg">
          <BadgeCheck className="size-5 text-primary" />
          {company.companyName}
        </CardTitle>
        <p className="text-xs text-muted-foreground">{t("licence", { number: company.licenseNumber ?? "" })}</p>
      </CardHeader>
      <CardContent className="space-y-4">
        {company.ratingCount ? (
          <div className="flex items-center gap-1.5 text-sm">
            <Star className="size-4 fill-amber-400 text-amber-400" />
            <span className="font-medium">{company.ratingAvg?.toFixed(1)}</span>
            <span className="text-muted-foreground">{t("reviewsCount", { count: company.ratingCount })}</span>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">{t("noReviews")}</p>
        )}

        {company.description && <p className="text-sm text-muted-foreground">{company.description}</p>}

        {company.addresses?.length ? (
          <div className="space-y-2 border-t border-border pt-3">
            {company.addresses.map((address) => (
              <div key={address.id} className="flex items-start gap-2 text-sm">
                <MapPin className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" />
                <div>
                  <p>
                    {address.cityName} — {address.addressText}
                  </p>
                  <a
                    href={`tel:${address.mobileNumber}`}
                    className="flex items-center gap-1 text-primary hover:underline"
                  >
                    <Phone className="size-3" /> {address.mobileNumber}
                  </a>
                </div>
              </div>
            ))}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
