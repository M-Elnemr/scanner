package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.application.CreateTripCommand;
import com.umrah.scanner.trip.application.RoomPriceInput;
import com.umrah.scanner.trip.application.TripHotelInput;
import com.umrah.scanner.trip.application.UpdateTripCommand;
import java.util.List;

/** Shared by {@link TripController} and {@link AdminTripController} so the two never drift. */
final class TripRequestMapper {

    private TripRequestMapper() {
    }

    static CreateTripCommand toCreateCommand(CreateTripRequest r) {
        return new CreateTripCommand(
                r.tripCode(), r.title(), r.departureDate(), r.returnDate(),
                r.outboundDepartureAirportId(), r.outboundArrivalAirportId(),
                r.returnDepartureAirportId(), r.returnArrivalAirportId(),
                r.airline(), r.transitCount(), r.transitCity(), r.transitDuration(),
                r.daysInMakkah(), r.daysInMadinah(), r.visaIncluded(), r.transportationIncluded(), r.mealsIncluded(),
                r.guideIncluded(), r.zamzamIncluded(), r.fastTrainIncluded(), r.description(), r.currencyId(),
                r.availableSeats(), toHotelInputs(r.hotels()), toRoomPriceInputs(r.prices()), r.tier());
    }

    static UpdateTripCommand toUpdateCommand(UpdateTripRequest r) {
        return new UpdateTripCommand(
                r.title(), r.departureDate(), r.returnDate(),
                r.outboundDepartureAirportId(), r.outboundArrivalAirportId(),
                r.returnDepartureAirportId(), r.returnArrivalAirportId(),
                r.airline(), r.transitCount(), r.transitCity(), r.transitDuration(),
                r.daysInMakkah(), r.daysInMadinah(), r.visaIncluded(), r.transportationIncluded(), r.mealsIncluded(),
                r.guideIncluded(), r.zamzamIncluded(), r.fastTrainIncluded(), r.description(), r.currencyId(),
                r.availableSeats(), toHotelInputs(r.hotels()), toRoomPriceInputs(r.prices()), r.tier());
    }

    // null means "omitted" (leave untouched on update); an empty list is an explicit clear.
    private static List<TripHotelInput> toHotelInputs(List<TripHotelRequest> hotels) {
        if (hotels == null) {
            return null;
        }
        return hotels.stream().map(h -> new TripHotelInput(h.hotelId(), h.freeBusIncluded())).toList();
    }

    // null means "omitted" (leave untouched on update); an empty list is an explicit clear.
    private static List<RoomPriceInput> toRoomPriceInputs(List<RoomPriceRequest> prices) {
        if (prices == null) {
            return null;
        }
        return prices.stream().map(p -> new RoomPriceInput(p.roomType(), p.price())).toList();
    }
}
