package com.umrah.scanner.trip.domain;

import com.umrah.scanner.airport.domain.Airport;
import com.umrah.scanner.common.domain.SoftDeletableEntity;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.currency.domain.Currency;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "trips")
@SQLRestriction("deleted_at is null")
// Every to-one association a trip response renders, in one query. Responses are mapped after the
// transaction closes (open-in-view is off), so anything omitted here fails at mapping time rather
// than at query time. Safe to combine with paging — none of these are collections.
//
// Each airport names its country through a subgraph. That is not optional: Spring Data's
// @EntityGraph defaults to FETCH semantics, which forces every attribute NOT listed here to be
// lazy — overriding the mapping's own fetch type. Leaving country implicit yields a proxy that
// blows up in AirportResponse the moment the session closes.
@NamedEntityGraph(
        name = Trip.GRAPH_WITH_REFERENCES,
        attributeNodes = {
                @NamedAttributeNode("company"),
                @NamedAttributeNode("currency"),
                @NamedAttributeNode(value = "outboundDepartureAirport", subgraph = "airportWithCountry"),
                @NamedAttributeNode(value = "outboundArrivalAirport", subgraph = "airportWithCountry"),
                @NamedAttributeNode(value = "returnDepartureAirport", subgraph = "airportWithCountry"),
                @NamedAttributeNode(value = "returnArrivalAirport", subgraph = "airportWithCountry")},
        subgraphs = @NamedSubgraph(name = "airportWithCountry", attributeNodes = @NamedAttributeNode("country")))
public class Trip extends SoftDeletableEntity {

    public static final String GRAPH_WITH_REFERENCES = "Trip.withReferences";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyProfile company;

    @Column(name = "trip_code", nullable = false, length = 50)
    private String tripCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    // A round trip has two legs and therefore four airports. The return leg is stated explicitly
    // rather than assumed to mirror the outbound, so a company can fly travellers out to Jeddah and
    // bring them home from Madinah.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbound_departure_airport_id", nullable = false)
    private Airport outboundDepartureAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbound_arrival_airport_id", nullable = false)
    private Airport outboundArrivalAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_departure_airport_id", nullable = false)
    private Airport returnDepartureAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_arrival_airport_id", nullable = false)
    private Airport returnArrivalAirport;

    @Column(name = "airline", nullable = false, length = 100)
    private String airline;

    @Column(name = "transit_count", nullable = false)
    private short transitCount;

    @Column(name = "transit_city", length = 100)
    private String transitCity;

    @Column(name = "transit_duration", length = 50)
    private String transitDuration;

    @Column(name = "days_in_makkah", nullable = false)
    private short daysInMakkah;

    @Column(name = "days_in_madinah", nullable = false)
    private short daysInMadinah;

    @Column(name = "visa_included", nullable = false)
    private boolean visaIncluded;

    @Column(name = "transportation_included", nullable = false)
    private boolean transportationIncluded;

    @Column(name = "meals_included", nullable = false)
    private boolean mealsIncluded;

    @Column(name = "guide_included", nullable = false)
    private boolean guideIncluded;

    @Column(name = "zamzam_included", nullable = false)
    private boolean zamzamIncluded;

    /** Haramain high-speed rail between Makkah and Madinah. */
    @Column(name = "fast_train_included", nullable = false)
    private boolean fastTrainIncluded;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    /**
     * Per-traveler commission override for this trip, in EGP. Null means "use the company's current
     * rate" — see {@link #effectiveCommissionPerTraveler()}. Set once at trip creation/edit; leads
     * still snapshot whatever this resolves to at creation time, so a later edit here or to the
     * company's own rate never repriced an existing lead.
     */
    @Column(name = "commission_per_traveler", precision = 10, scale = 2)
    private BigDecimal commissionPerTraveler;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TripStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private TripTier tier;

    @Column(name = "last_update", nullable = false)
    private Instant lastUpdate;

    /** Postgres-computed (return_date - departure_date); read-only, backs the browse days-range filter. */
    @Column(name = "duration_days", nullable = false, insertable = false, updatable = false)
    private int durationDays;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("city asc")
    private List<TripHotel> hotels = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roomType asc")
    private List<RoomPrice> roomPrices = new ArrayList<>();

    /** The rate actually used to price a lead: this trip's own override if set, else the company's. */
    public BigDecimal effectiveCommissionPerTraveler() {
        return commissionPerTraveler != null ? commissionPerTraveler : company.getCommissionPerTraveler();
    }

    public void addHotel(TripHotel hotel) {
        hotels.add(hotel);
        hotel.setTrip(this);
    }

    public void addRoomPrice(RoomPrice roomPrice) {
        roomPrices.add(roomPrice);
        roomPrice.setTrip(this);
    }
}
