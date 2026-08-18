import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { listAirports, listApprovedCompanies, listCurrencies } from "@/lib/admin/reference-data";
import { listHotelsByCity } from "@/lib/admin/hotel-picker";
import { TripForm } from "@/components/admin/trip-form";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("tripNew") };
}

export default async function NewTripPage() {
  const t = await getTranslations("admin.trips");
  const [airports, currencies, companies, makkahHotels, madinahHotels] = await Promise.all([
    listAirports(),
    listCurrencies(),
    listApprovedCompanies(),
    listHotelsByCity("MAKKAH"),
    listHotelsByCity("MADINAH"),
  ]);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="font-heading text-2xl font-bold tracking-tight">{t("new")}</h1>
      <TripForm
        mode="create"
        airports={airports}
        currencies={currencies}
        companies={companies}
        makkahHotels={makkahHotels}
        madinahHotels={madinahHotels}
        initial={{}}
      />
    </div>
  );
}
