package com.umrah.scanner.user.domain;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at is null")
public class User extends SoftDeletableEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "google_sub", nullable = false, length = 255)
    private String googleSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;
}
