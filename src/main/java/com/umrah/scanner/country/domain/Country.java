package com.umrah.scanner.country.domain;

import com.umrah.scanner.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fixed reference list — seeded via migration, never user-editable.
 *
 * <p>Exists so an airport can say which country it belongs to. Today that is only Egypt and Saudi
 * Arabia, but routing rules are written against the country relationship rather than against those
 * two names, so opening the platform to another origin country is a matter of inserting rows.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "countries")
public class Country extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /** ISO 3166-1 alpha-2, e.g. EG, SA. */
    @Column(name = "iso2", nullable = false, unique = true, length = 2)
    private String iso2;
}
