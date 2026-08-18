import { getTranslations } from "next-intl/server";
import { publicApi, unwrap } from "@/lib/api/client";
import { pageableQuery } from "@/lib/api/pageable";
import { TripGrid } from "@/components/trip/trip-grid";

export async function FeaturedTrips() {
  const result = await publicApi.GET("/api/v1/trips", {
    params: { query: pageableQuery(0, 8) },
    cache: "force-cache",
    next: { revalidate: 30 },
  });
  const page = unwrap(result);

  if (!page.content?.length) {
    const t = await getTranslations("home");
    return <p className="text-muted-foreground">{t("featuredEmpty")}</p>;
  }

  return <TripGrid trips={page.content} />;
}
