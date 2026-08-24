package com.umrah.scanner.trip.infrastructure;

import com.umrah.scanner.company.domain.CompanyAddress;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.domain.TripTier;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic filter predicates for the public trip-browse query and the admin trip console. */
public final class TripSpecifications {

    private TripSpecifications() {
    }

    /**
     * Suspending or rejecting a company does not touch its trips, so without this a suspended
     * company's PUBLISHED trips stayed visible in the public browse — a pre-existing gap this
     * closes. A customer should never be able to reach a company that cannot be contacted.
     */
    public static Specification<Trip> companyIsApproved() {
        return (root, query, cb) -> cb.equal(root.get("company").get("status"), CompanyStatus.APPROVED);
    }

    public static Specification<Trip> hasCompanyId(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }

    public static Specification<Trip> titleOrCodeContains(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("tripCode")), pattern));
    }

    public static Specification<Trip> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<Trip> hasStatus(TripStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Trip> hasTierIn(List<TripTier> tiers) {
        return (root, query, cb) -> root.get("tier").in(tiers);
    }

    /** Hides trips whose departure date has already passed from discovery (browse/home). Not
     * applied to single-trip lookups — see the carve-out comment at its call site. */
    public static Specification<Trip> departureOnOrAfter(LocalDate date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("departureDate"), date);
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

    /** Matches trips departing (and returning) from this Egyptian airport. */
    public static Specification<Trip> hasOutboundDepartureAirport(UUID airportId) {
        return (root, query, cb) -> cb.equal(root.get("outboundDepartureAirport").get("id"), airportId);
    }

    /** Matches trips whose organizing company has at least one branch address in {@code cityId}. */
    public static Specification<Trip> companyInCity(UUID cityId) {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            var address = subquery.from(CompanyAddress.class);
            subquery.select(cb.literal(1));
            subquery.where(
                    cb.equal(address.get("company"), root.get("company")),
                    cb.equal(address.get("city").get("id"), cityId));
            return cb.exists(subquery);
        };
    }

    /**
     * Orders results by the given room type's price. A left join keeps trips with no price row for
     * that room type in the result set (sorted to whichever end the database puts nulls on) instead
     * of silently dropping them.
     *
     * <p>Sets order directly on the shared {@code CriteriaQuery} rather than returning a predicate —
     * this only works because callers never also populate {@code Pageable}'s own {@code Sort} on the
     * same query; Spring Data JPA overwrites {@code query.orderBy(...)} with {@code Pageable}'s sort
     * whenever one is present, which would silently undo this.
     */
    public static Specification<Trip> orderByRoomTypePrice(RoomType roomType, Sort.Direction direction) {
        return (root, query, cb) -> {
            Join<Trip, RoomPrice> prices = root.join("roomPrices", JoinType.LEFT);
            prices.on(cb.equal(prices.get("roomType"), roomType));
            query.orderBy(direction == Sort.Direction.DESC ? cb.desc(prices.get("price")) : cb.asc(prices.get("price")));
            return cb.conjunction();
        };
    }
}
