package com.umrah.scanner.common.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.Hibernate;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity that)) {
            return false;
        }
        return id != null && id.equals(that.id) && Hibernate.getClass(this) == Hibernate.getClass(that);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
