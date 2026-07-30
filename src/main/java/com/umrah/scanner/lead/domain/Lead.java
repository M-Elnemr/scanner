package com.umrah.scanner.lead.domain;

import com.umrah.scanner.common.domain.BaseEntity;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.Trip;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leads", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "trip_id"}))
@EntityListeners(AuditingEntityListener.class)
public class Lead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyProfile company;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LeadStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmed_room_type", length = 20)
    private RoomType confirmedRoomType;

    @Column(name = "confirmed_price", precision = 10, scale = 2)
    private BigDecimal confirmedPrice;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
