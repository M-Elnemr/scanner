package com.umrah.scanner.trip.domain;

import com.umrah.scanner.common.domain.SoftDeletableEntity;
import com.umrah.scanner.company.domain.CompanyProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
public class Trip extends SoftDeletableEntity {

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

    @Column(name = "departure_airport", nullable = false, length = 10)
    private String departureAirport;

    @Column(name = "arrival_airport", nullable = false, length = 10)
    private String arrivalAirport;

    @Column(name = "airline", nullable = false, length = 100)
    private String airline;

    @Column(name = "flight_number", length = 20)
    private String flightNumber;

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

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TripStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private TripTier tier;

    @Column(name = "last_update", nullable = false)
    private Instant lastUpdate;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("city asc")
    private List<TripHotel> hotels = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roomType asc")
    private List<RoomPrice> roomPrices = new ArrayList<>();

    public void addHotel(TripHotel hotel) {
        hotels.add(hotel);
        hotel.setTrip(this);
    }

    public void addRoomPrice(RoomPrice roomPrice) {
        roomPrices.add(roomPrice);
        roomPrice.setTrip(this);
    }
}
