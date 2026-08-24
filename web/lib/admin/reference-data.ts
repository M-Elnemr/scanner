import { publicApi } from "@/lib/api/client";
import { apiClient } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";

export interface AirportOption {
  id: string;
  iataCode: string;
  city: string;
  cityAr?: string;
  countryName: string;
  countryNameAr?: string;
  countryIso2?: string;
}

export interface CurrencyOption {
  id: string;
  code: string;
  symbol: string;
}

export interface CompanyOption {
  id: string;
  name: string;
  commissionPerTraveler: number;
}

export interface TripOption {
  id: string;
  title: string;
  tripCode: string;
}

/** Every trip (any status, any company) for the admin lead console's trip filter — optionally scoped to one company. */
export async function listTrips(companyId?: string): Promise<TripOption[]> {
  const api = await apiClient();
  const result = await api.GET("/api/v1/admin/trips", {
    params: { query: { ...(companyId ? { companyId } : {}), ...pageableQuery(0, 500, "title,asc") } },
    cache: "no-store",
  });
  return (result.data?.data?.content ?? [])
    .filter((t) => t.id)
    .map((t) => ({ id: t.id!, title: t.title ?? "", tripCode: t.tripCode ?? "" }));
}

export async function listAirports(): Promise<AirportOption[]> {
  const result = await publicApi.GET("/api/v1/airports", { cache: "force-cache", next: { revalidate: 3600 } });
  return (result.data?.data ?? [])
    .filter((a) => a.id)
    .map((a) => ({
      id: a.id!,
      iataCode: a.iataCode ?? "?",
      city: a.city ?? "",
      cityAr: a.cityAr,
      countryName: a.countryName ?? "",
      countryNameAr: a.countryNameAr,
      countryIso2: a.countryIso2,
    }));
}

export async function listCurrencies(): Promise<CurrencyOption[]> {
  const result = await publicApi.GET("/api/v1/currencies", { cache: "force-cache", next: { revalidate: 3600 } });
  return (result.data?.data ?? []).filter((c) => c.id).map((c) => ({ id: c.id!, code: c.code ?? "", symbol: c.symbol ?? "" }));
}

export async function listApprovedCompanies(): Promise<CompanyOption[]> {
  const api = await apiClient();
  const result = await api.GET("/api/v1/admin/companies", {
    params: { query: { status: "APPROVED", ...pageableQuery(0, 200, "companyName,asc") } },
    cache: "no-store",
  });
  return (result.data?.data?.content ?? [])
    .filter((c) => c.id)
    .map((c) => ({ id: c.id!, name: c.companyName ?? "", commissionPerTraveler: c.commissionPerTraveler ?? 0 }));
}
