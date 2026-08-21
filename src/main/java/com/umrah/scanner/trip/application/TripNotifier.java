package com.umrah.scanner.trip.application;

import com.umrah.scanner.hotel.domain.HotelCity;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripHotel;
import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Turns a trip becoming available into the notification every customer needs to see. Kept apart
 * from the use cases that create/publish a trip so adding or retargeting the message never
 * touches that logic, mirroring {@link com.umrah.scanner.lead.application.LeadNotifier}.
 */
@Service
public class TripNotifier {

    private final NotificationDispatcher notificationDispatcher;
    private final UserRepository userRepository;
    private final String baseUrl;

    public TripNotifier(
            NotificationDispatcher notificationDispatcher,
            UserRepository userRepository,
            @Value("${app.base-url}") String baseUrl) {
        this.notificationDispatcher = notificationDispatcher;
        this.userRepository = userRepository;
        this.baseUrl = baseUrl;
    }

    /** A trip just became visible/bookable; every customer is told about it. */
    public void tripPublished(Trip trip) {
        Map<String, Object> data = new HashMap<>();
        data.put("tripId", trip.getId().toString());
        resolveImageUrl(trip).ifPresent(url -> data.put("imageUrl", url));

        String title = "رحلة جديدة متاحة";
        String body = "تمت إضافة رحلة جديدة \"" + trip.getTitle() + "\" — تصفحها الآن!";

        for (User customer : userRepository.findAllByRole(Role.CUSTOMER)) {
            notificationDispatcher.dispatch(customer.getId(), "NEW_TRIP", title, body, data);
        }
    }

    /** The Makkah hotel's photo, falling back to Madinah's, made absolute via {@code app.base-url}. */
    private Optional<String> resolveImageUrl(Trip trip) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return Optional.empty();
        }
        List<TripHotel> hotels = trip.getHotels();
        return photoUrlForCity(hotels, HotelCity.MAKKAH)
                .or(() -> photoUrlForCity(hotels, HotelCity.MADINAH))
                .map(this::toAbsoluteUrl);
    }

    private Optional<String> photoUrlForCity(List<TripHotel> hotels, HotelCity city) {
        return hotels.stream()
                .filter(tripHotel -> tripHotel.getCity() == city)
                .map(tripHotel -> tripHotel.getHotel().getPhotoUrl())
                .filter(photoUrl -> photoUrl != null && !photoUrl.isBlank())
                .findFirst();
    }

    private String toAbsoluteUrl(String photoUrl) {
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            return photoUrl;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = photoUrl.startsWith("/") ? photoUrl : "/" + photoUrl;
        return base + path;
    }
}
