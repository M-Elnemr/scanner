package com.umrah.scanner.trip.domain;

import com.umrah.scanner.common.domain.BaseEntity;
import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.domain.HotelCity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Which catalogue {@link Hotel} a trip uses in one city. Everything about the hotel itself — name,
 * stars, distance, walkability, free shuttle — lives on {@code Hotel}; this row is purely the join.
 *
 * <p>{@code city} is denormalized here on purpose so {@code uq_trip_hotels_trip_city} (one hotel per
 * city per trip) stays a simple two-column index. It can never disagree with {@code hotel.getCity()}
 * because the database proves it: {@code fk_trip_hotels_hotel} is a composite FK on
 * {@code (hotel_id, city)} against {@code hotels(id, city)}, not just {@code hotel_id} alone.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "trip_hotels")
@EntityListeners(AuditingEntityListener.class)
public class TripHotel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Enumerated(EnumType.STRING)
    @Column(name = "city", nullable = false, length = 20)
    private HotelCity city;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
