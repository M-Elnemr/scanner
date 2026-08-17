package com.umrah.scanner.hotel.domain;

import com.umrah.scanner.common.domain.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * A Makkah or Madinah hotel the platform curates. Unlike the other reference lists (cities,
 * airports, currencies) this one is admin-editable after seeding, which is why it carries audit and
 * soft-delete columns the others don't.
 *
 * <p>{@code deleted_at} is reserved for a hotel nothing references — see {@code DeleteHotelUseCase}.
 * Retiring a hotel that trips still use is {@link #active} instead, because {@code trip_hotels.hotel}
 * is a non-optional association: a soft-deleted-but-referenced row would make every trip using it
 * unloadable under this class's own {@code @SQLRestriction}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "hotels")
@SQLRestriction("deleted_at is null")
public class Hotel extends SoftDeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "city", nullable = false, length = 20)
    private HotelCity city;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "name_ar", length = 255)
    private String nameAr;

    @Column(name = "stars", nullable = false)
    private short stars;

    /** Distance to the Haram (Makkah) or the Prophet's Mosque (Madinah), whichever {@link #city} is. */
    @Column(name = "distance_to_haram_m")
    private Integer distanceToHaramM;

    /** An admin's judgement call, not a computation over {@link #distanceToHaramM} — see the migration. */
    @Column(name = "can_walk", nullable = false)
    private boolean canWalk;

    @Column(name = "location_url", length = 500)
    private String locationUrl;

    /** false retires the hotel from the picker for new trips without touching trips already using it. */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
