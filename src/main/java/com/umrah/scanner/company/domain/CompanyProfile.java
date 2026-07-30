package com.umrah.scanner.company.domain;

import com.umrah.scanner.common.domain.SoftDeletableEntity;
import com.umrah.scanner.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "company_profiles")
@SQLRestriction("deleted_at is null")
public class CompanyProfile extends SoftDeletableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "license_number", nullable = false, length = 100)
    private String licenseNumber;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "whatsapp", length = 30)
    private String whatsapp;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CompanyStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;
}
