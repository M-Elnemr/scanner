package com.umrah.scanner.city.domain;

import com.umrah.scanner.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fixed reference list of Egypt governorates/cities — seeded via migration, never user-editable. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cities")
public class City extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
}
