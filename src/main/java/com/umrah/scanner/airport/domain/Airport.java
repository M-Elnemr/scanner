package com.umrah.scanner.airport.domain;

import com.umrah.scanner.common.domain.BaseEntity;
import com.umrah.scanner.country.domain.Country;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fixed reference list of airports the platform flies between — seeded via migration, never user-editable. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "airports")
public class Airport extends BaseEntity {

    /**
     * EAGER by deliberate exception to the codebase default. Airports are immutable reference data —
     * six rows across two countries — and every read of one renders its country, so a lazy proxy
     * buys nothing here and only creates a chance of a LazyInitializationException at mapping time.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    /** IATA code, e.g. CAI, JED. */
    @Column(name = "iata_code", nullable = false, unique = true, length = 3)
    private String iataCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** The city served, which is what a traveller actually picks by. */
    @Column(name = "city", nullable = false, length = 100)
    private String city;
}
