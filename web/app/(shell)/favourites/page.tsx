import type { Metadata } from "next";
import { Heart } from "lucide-react";
import { apiClient, requireUser } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { TripGrid } from "@/components/trip/trip-grid";

export const metadata: Metadata = { title: "Favourites" };

export default async function FavouritesPage() {
  const user = await requireUser();

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center text-muted-foreground">
        Favourites are available to customer accounts.
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/customers/me/favourites", {
    params: { query: pageableQuery(0, 50) },
    cache: "no-store",
  });
  const trips = (result.data?.data?.content ?? []).map((f) => f.trip).filter((t) => t != null);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="mb-8 font-heading text-3xl font-bold tracking-tight">Favourites</h1>

      {trips.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-20 text-center">
          <Heart className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-muted-foreground">Trips you save will show up here.</p>
        </div>
      ) : (
        <TripGrid trips={trips} />
      )}
    </div>
  );
}
