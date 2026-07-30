package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.application.TripDetailResult;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripDetailResponse(
        UUID id,
        String tripCode,
        String title,
        LocalDate departureDate,
        LocalDate returnDate,
        String departureAirport,
        String arrivalAirport,
        String airline,
        String flightNumber,
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
        String description,
        String currency,
        int availableSeats,
        TripStatus status,
        Instant lastUpdate,
        List<TripHotelResponse> hotels,
        List<RoomPriceResponse> roomPrices,
        CompanyContact company) {

    public record CompanyContact(UUID companyId, String companyName, String whatsapp, String logoUrl) {
    }

    /** companyVisible gates whether {@code company} is populated — never leak identity to a browsing customer. */
    public static TripDetailResponse from(Trip trip, boolean companyVisible) {
        CompanyContact contact = companyVisible
                ? new CompanyContact(trip.getCompany().getId(), trip.getCompany().getCompanyName(),
                        trip.getCompany().getWhatsapp(), trip.getCompany().getLogoUrl())
                : null;

        return new TripDetailResponse(
                trip.getId(), trip.getTripCode(), trip.getTitle(), trip.getDepartureDate(), trip.getReturnDate(),
                trip.getDepartureAirport(), trip.getArrivalAirport(), trip.getAirline(), trip.getFlightNumber(),
                trip.getTransitCount(), trip.getTransitCity(), trip.getTransitDuration(),
                trip.getDaysInMakkah(), trip.getDaysInMadinah(),
                trip.isVisaIncluded(), trip.isTransportationIncluded(), trip.isMealsIncluded(),
                trip.isGuideIncluded(), trip.isZamzamIncluded(), trip.getDescription(),
                trip.getCurrency(), trip.getAvailableSeats(), trip.getStatus(), trip.getLastUpdate(),
                trip.getHotels().stream().map(TripHotelResponse::from).toList(),
                trip.getRoomPrices().stream().map(RoomPriceResponse::from).toList(),
                contact);
    }

    public static TripDetailResponse from(TripDetailResult result) {
        return from(result.trip(), result.companyVisible());
    }

    /** For the owning company's own edit view — always shows its own identity. */
    public static TripDetailResponse ownedBy(Trip trip) {
        return from(trip, true);
    }
}
