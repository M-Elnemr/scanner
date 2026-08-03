package com.umrah.scanner.currency.domain;

import com.umrah.scanner.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fixed reference list of currencies a company may price a trip in — seeded via migration, never
 * user-editable. A trip points at one of these; its room prices inherit it.
 *
 * <p>Carries no exchange rate on purpose. A price is quoted and paid in the trip's own currency, so
 * nothing in the system converts between them; adding rates later would be a separate concern with
 * its own history table rather than a column here.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "currencies")
public class Currency extends BaseEntity {

    /** ISO 4217, e.g. EGP, SAR, USD. */
    @Column(name = "code", nullable = false, unique = true, length = 3)
    private String code;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "symbol", nullable = false, length = 8)
    private String symbol;
}
