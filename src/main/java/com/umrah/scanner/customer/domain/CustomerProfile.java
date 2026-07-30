package com.umrah.scanner.customer.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer_profiles")
@SQLRestriction("deleted_at is null")
public class CustomerProfile extends SoftDeletableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "cashback_wallet_number", length = 30)
    private String cashbackWalletNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", length = 20)
    private WalletType walletType;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;
}
