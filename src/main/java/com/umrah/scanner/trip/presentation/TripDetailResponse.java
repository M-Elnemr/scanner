package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.airport.presentation.AirportResponse;
import com.umrah.scanner.currency.presentation.CurrencyResponse;
import com.umrah.scanner.trip.application.TripDetailResult;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.domain.TripTier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(name = "TripDetail", description = "Full Umrah program details as shown on the program page.")
public record TripDetailResponse(
        UUID id,
        String tripCode,
        String title,
        LocalDate departureDate,
        LocalDate returnDate,
        @Schema(description = "Leg 1 origin, in the traveler's own country") AirportResponse outboundDepartureAirport,
        @Schema(description = "Leg 1 destination, in Saudi Arabia") AirportResponse outboundArrivalAirport,
        @Schema(description = "Leg 2 origin — not necessarily the airport they landed at") AirportResponse returnDepartureAirport,
        @Schema(description = "Leg 2 destination, back home") AirportResponse returnArrivalAirport,
        String airline,
        short transitCount,
        String transitCity,
        String transitDuration,
        short daysInMakkah,
        short daysInMadinah,
        boolean visaIncluded,
        boolean transportationIncluded,
        boolean mealsIncluded,
        boolean guideIncluded,
        boolean zamzamIncluded,
        @Schema(description = "Haramain high-speed rail between Makkah and Madinah") boolean fastTrainIncluded,
        String description,
        CurrencyResponse currency,
        int availableSeats,
        TripStatus status,
        TripTier tier,
        @Schema(description = "EGP each traveler earns back after the trip is paid for and the company settles up",
                example = "500.00")
        BigDecimal cashbackPerTraveler,
        @Schema(description = "This trip's commission-per-traveler override, in EGP. Null means it uses the "
                + "company's own rate. Admin/company-only — never present on the public/customer view.")
        BigDecimal commissionPerTraveler,
        Instant lastUpdate,
        List<TripHotelResponse> hotels,
        List<RoomPriceResponse> roomPrices,
        CompanyContact company) {

    public record CompanyContact(UUID companyId, String companyName, String whatsapp) {
    }

    /**
     * companyVisible gates whether {@code company} is populated — never leak identity to a browsing
     * customer. includeCommission gates {@code commissionPerTraveler} the same way: true for the
     * company/admin-owned reads, false for the public/customer one. Customers only ever see the
     * cashback that derives from it, never the commission itself.
     */
    private static TripDetailResponse from(
            Trip trip, boolean companyVisible, BigDecimal cashbackPerTraveler, boolean includeCommission) {
        CompanyContact contact = companyVisible
                ? new CompanyContact(trip.getCompany().getId(), trip.getCompany().getCompanyName(),
                        trip.getCompany().getWhatsapp())
                : null;

        return new TripDetailResponse(
                trip.getId(), trip.getTripCode(), trip.getTitle(), trip.getDepartureDate(), trip.getReturnDate(),
                AirportResponse.from(trip.getOutboundDepartureAirport()),
                AirportResponse.from(trip.getOutboundArrivalAirport()),
                AirportResponse.from(trip.getReturnDepartureAirport()),
                AirportResponse.from(trip.getReturnArrivalAirport()),
                trip.getAirline(),
                trip.getTransitCount(), trip.getTransitCity(), trip.getTransitDuration(),
                trip.getDaysInMakkah(), trip.getDaysInMadinah(),
                trip.isVisaIncluded(), trip.isTransportationIncluded(), trip.isMealsIncluded(),
                trip.isGuideIncluded(), trip.isZamzamIncluded(), trip.isFastTrainIncluded(), trip.getDescription(),
                CurrencyResponse.from(trip.getCurrency()), trip.getAvailableSeats(), trip.getStatus(), trip.getTier(),
                cashbackPerTraveler, includeCommission ? trip.getCommissionPerTraveler() : null, trip.getLastUpdate(),
                trip.getHotels().stream().map(TripHotelResponse::from).toList(),
                trip.getRoomPrices().stream().map(RoomPriceResponse::from).toList(),
                contact);
    }

    public static TripDetailResponse from(TripDetailResult result) {
        return from(result.trip(), result.companyVisible(), result.cashbackPerTraveler(), result.includeCommission());
    }
}
