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
}

export interface CurrencyOption {
  id: string;
  code: string;
  symbol: string;
}

export interface CompanyOption {
  id: string;
  name: string;
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
  return (result.data?.data?.content ?? []).filter((c) => c.id).map((c) => ({ id: c.id!, name: c.companyName ?? "" }));
}
