import type { Metadata } from "next";
import { listAirports, listApprovedCompanies, listCurrencies } from "@/lib/admin/reference-data";
import { listHotelsByCity } from "@/lib/admin/hotel-picker";
import { TripForm } from "@/components/admin/trip-form";

export const metadata: Metadata = { title: "New trip · Admin" };

export default async function NewTripPage() {
  const [airports, currencies, companies, makkahHotels, madinahHotels] = await Promise.all([
    listAirports(),
    listCurrencies(),
    listApprovedCompanies(),
    listHotelsByCity("MAKKAH"),
    listHotelsByCity("MADINAH"),
  ]);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="font-heading text-2xl font-bold tracking-tight">New trip</h1>
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
