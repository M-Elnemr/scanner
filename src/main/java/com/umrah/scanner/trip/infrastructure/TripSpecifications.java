package com.umrah.scanner.trip.infrastructure;

import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.domain.TripTier;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic filter predicates for the public trip-browse query. */
public final class TripSpecifications {

    private TripSpecifications() {
    }

    public static Specification<Trip> hasStatus(TripStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Trip> hasTierIn(List<TripTier> tiers) {
        return (root, query, cb) -> root.get("tier").in(tiers);
    }

    public static Specification<Trip> departureBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("departureDate"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("departureDate"), from);
            }
            return cb.lessThanOrEqualTo(root.get("departureDate"), to);
        };
    }

    public static Specification<Trip> durationBetween(Integer minDays, Integer maxDays) {
        return (root, query, cb) -> {
            if (minDays != null && maxDays != null) {
                return cb.between(root.get("durationDays"), minDays, maxDays);
            }
            if (minDays != null) {
                return cb.greaterThanOrEqualTo(root.get("durationDays"), minDays);
            }
            return cb.lessThanOrEqualTo(root.get("durationDays"), maxDays);
        };
    }

    /** Matches trips that have a room price for {@code roomType} within the given (optional) bounds. */
    public static Specification<Trip> priceForRoomTypeBetween(RoomType roomType, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            var roomPrice = subquery.from(RoomPrice.class);
            subquery.select(cb.literal(1));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(roomPrice.get("trip"), root));
            predicates.add(cb.equal(roomPrice.get("roomType"), roomType));
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(roomPrice.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(roomPrice.get("price"), maxPrice));
            }
            subquery.where(predicates.toArray(new Predicate[0]));

            return cb.exists(subquery);
        };
    }
}
