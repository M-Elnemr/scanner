import { publicApi } from "@/lib/api/client";

export interface HotelOption {
  id: string;
  name: string;
}

export async function listHotelsByCity(city: "MAKKAH" | "MADINAH"): Promise<HotelOption[]> {
  const result = await publicApi.GET("/api/v1/hotels", { params: { query: { city } }, cache: "no-store" });
  return (result.data?.data ?? []).filter((h) => h.id && h.name).map((h) => ({ id: h.id!, name: h.name! }));
}
