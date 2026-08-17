import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { apiClient } from "@/lib/auth/server";
import { listAirports, listApprovedCompanies, listCurrencies } from "@/lib/admin/reference-data";
import { listHotelsByCity } from "@/lib/admin/hotel-picker";
import { TripForm } from "@/components/admin/trip-form";
import { TripStatusBadge } from "@/components/admin/trip-status-badge";
import { TripRowActions } from "@/components/admin/trip-row-actions";

export const metadata: Metadata = { title: "Edit trip · Admin" };

export default async function EditTripPage(props: PageProps<"/admin/trips/[id]">) {
  const { id } = await props.params;
  const api = await apiClient();
  const [tripResult, airports, currencies, companies, makkahHotels, madinahHotels] = await Promise.all([
    api.GET("/api/v1/admin/trips/{id}", { params: { path: { id } }, cache: "no-store" }),
    listAirports(),
    listCurrencies(),
    listApprovedCompanies(),
    listHotelsByCity("MAKKAH"),
    listHotelsByCity("MADINAH"),
  ]);
  const trip = tripResult.data?.data;
  if (!trip?.id) notFound();

  const makkah = trip.hotels?.find((h) => h.city === "MAKKAH");
  const madinah = trip.hotels?.find((h) => h.city === "MADINAH");
  const priceFor = (type: string) => trip.roomPrices?.find((p) => p.roomType === type)?.price;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">{trip.title}</h1>
          <div className="mt-1 flex items-center gap-2">
            <TripStatusBadge status={trip.status} />
            <span className="text-xs text-muted-foreground">{trip.company?.companyName}</span>
          </div>
        </div>
        <TripRowActions id={trip.id} status={trip.status} companies={companies} />
      </div>

      <TripForm
        mode="edit"
        tripId={trip.id}
        airports={airports}
        currencies={currencies}
        companies={companies}
        makkahHotels={makkahHotels}
        madinahHotels={madinahHotels}
        initial={{
          tripCode: trip.tripCode ?? "",
          title: trip.title ?? "",
          departureDate: trip.departureDate ?? "",
          returnDate: trip.returnDate ?? "",
          outboundDepartureAirportId: trip.outboundDepartureAirport?.id ?? "",
          outboundArrivalAirportId: trip.outboundArrivalAirport?.id ?? "",
          returnDepartureAirportId: trip.returnDepartureAirport?.id ?? "",
          returnArrivalAirportId: trip.returnArrivalAirport?.id ?? "",
          airline: trip.airline ?? "",
          transitCount: trip.transitCount != null ? String(trip.transitCount) : "",
          transitCity: trip.transitCity ?? "",
          transitDuration: trip.transitDuration ?? "",
          daysInMakkah: trip.daysInMakkah != null ? String(trip.daysInMakkah) : "",
          daysInMadinah: trip.daysInMadinah != null ? String(trip.daysInMadinah) : "",
          visaIncluded: trip.visaIncluded ?? false,
          transportationIncluded: trip.transportationIncluded ?? false,
          mealsIncluded: trip.mealsIncluded ?? false,
          guideIncluded: trip.guideIncluded ?? false,
          zamzamIncluded: trip.zamzamIncluded ?? false,
          fastTrainIncluded: trip.fastTrainIncluded ?? false,
          description: trip.description ?? "",
          currencyId: trip.currency?.id ?? "",
          availableSeats: trip.availableSeats != null ? String(trip.availableSeats) : "",
          tier: trip.tier ?? "ECONOMIC",
          makkahHotelId: makkah?.hotel?.id ?? "",
          makkahFreeBus: makkah?.freeBusIncluded ?? false,
          madinahHotelId: madinah?.hotel?.id ?? "",
          madinahFreeBus: madinah?.freeBusIncluded ?? false,
          prices: {
            SINGLE: priceFor("SINGLE") != null ? String(priceFor("SINGLE")) : "",
            DOUBLE: priceFor("DOUBLE") != null ? String(priceFor("DOUBLE")) : "",
            TRIPLE: priceFor("TRIPLE") != null ? String(priceFor("TRIPLE")) : "",
            QUAD: priceFor("QUAD") != null ? String(priceFor("QUAD")) : "",
            CHILD: priceFor("CHILD") != null ? String(priceFor("CHILD")) : "",
            INFANT: priceFor("INFANT") != null ? String(priceFor("INFANT")) : "",
          },
        }}
      />
    </div>
  );
}
