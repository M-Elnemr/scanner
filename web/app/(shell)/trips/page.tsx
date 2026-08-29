import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { publicApi, unwrap } from "@/lib/api/client";
import { pageableQuery } from "@/lib/api/pageable";
import { listCities } from "@/lib/admin/cities";
import { listAirports } from "@/lib/admin/reference-data";
import { FilterBar } from "@/components/trip/filter-bar";
import { TripGrid } from "@/components/trip/trip-grid";
import { TripPagination } from "@/components/trip/trip-pagination";
import { CompareTray } from "@/components/trip/compare-tray";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("trips");
  return { title: t("title") };
}

function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function many(value: string | string[] | undefined): string[] {
  if (!value) return [];
  return Array.isArray(value) ? value : [value];
}

export default async function TripsPage(props: PageProps<"/trips">) {
  const t = await getTranslations("trips");
  const sp = await props.searchParams;
  const page = Number(first(sp.page) ?? 0);

  const tiers = many(sp.tiers) as ("VIP" | "PREMIUM" | "ECONOMIC")[];
  const roomSize = first(sp.roomSize) ? Number(first(sp.roomSize)) : undefined;
  const minPrice = first(sp.minPrice) ? Number(first(sp.minPrice)) : undefined;
  const maxPrice = first(sp.maxPrice) ? Number(first(sp.maxPrice)) : undefined;
  const minDays = first(sp.minDays) ? Number(first(sp.minDays)) : undefined;
  const maxDays = first(sp.maxDays) ? Number(first(sp.maxDays)) : undefined;
  const departureFrom = first(sp.departureFrom);
  const departureTo = first(sp.departureTo);
  const cityId = first(sp.cityId);
  const departureAirportId = first(sp.departureAirportId);
  const priceSort = first(sp.priceSort) as "asc" | "desc" | undefined;
  const durationSort = first(sp.durationSort) as "asc" | "desc" | undefined;

  const [result, cities, airports] = await Promise.all([
    publicApi.GET("/api/v1/trips", {
      params: {
        query: {
          ...(tiers.length ? { tiers } : {}),
          roomSize,
          minPrice,
          maxPrice,
          minDays,
          maxDays,
          departureFrom,
          departureTo,
          cityId,
          departureAirportId,
          priceSort,
          durationSort,
          ...pageableQuery(page, 12),
        },
      },
      cache: "force-cache",
      next: { revalidate: 30 },
    }),
    listCities(),
    listAirports(),
  ]);
  const result_ = unwrap(result);
  // Trips only ever depart from and return to Egypt (the other leg is Saudi Arabia), so the
  // filter only ever needs to offer Egyptian airports.
  const departureAirports = airports.filter((a) => a.countryIso2 === "EG");

  const searchParams = new URLSearchParams();
  Object.entries(sp).forEach(([key, value]) => {
    if (key === "page") return;
    for (const v of many(value)) searchParams.append(key, v);
  });

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <div className="mb-8 flex flex-col gap-4">
        <div>
          <h1 className="font-heading text-3xl font-bold tracking-tight">{t("title")}</h1>
          <p className="text-muted-foreground">
            {t("journeysAvailable", { count: result_.totalElements ?? 0 })}
          </p>
        </div>
        <FilterBar cities={cities} airports={departureAirports} />
      </div>

      {result_.content?.length ? (
        <>
          <TripGrid trips={result_.content} />
          <TripPagination
            page={result_.page ?? 0}
            totalPages={result_.totalPages ?? 1}
            basePath="/trips"
            searchParams={searchParams}
          />
        </>
      ) : (
        <div className="rounded-2xl border border-dashed border-border py-24 text-center text-muted-foreground">
          {t("noResults")}
        </div>
      )}

      <CompareTray />
    </div>
  );
}
