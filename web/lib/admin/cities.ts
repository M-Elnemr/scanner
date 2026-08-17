import { publicApi } from "@/lib/api/client";

export interface CityOption {
  id: string;
  name: string;
}

export async function listCities(): Promise<CityOption[]> {
  const result = await publicApi.GET("/api/v1/cities", { cache: "force-cache", next: { revalidate: 3600 } });
  return (result.data?.data ?? []).filter((c) => c.id && c.name).map((c) => ({ id: c.id!, name: c.name! }));
}
