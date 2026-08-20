package com.umrah.scanner.lead.domain;

import com.umrah.scanner.common.domain.BaseEntity;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.pricing.domain.LeadPricing;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.Trip;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A booking request: one customer, one trip, and the money that follows from it.
 *
 * <p>The commission and cashback figures here are a <em>snapshot</em> taken once at creation. They
 * are never recomputed, so a later change to the company's rate or to the cashback rules leaves
 * every existing lead exactly as it was priced.
 *
 * <p>Uniqueness is not declared here: both rules that guard this table are <em>partial</em> indexes
 * ("one live lead per customer/trip pair", "one live lead per customer"), which JPA cannot express.
 * They live in {@code V31__lead_cancellation.sql} instead, and a plain {@code @UniqueConstraint}
 * here would only document a constraint the database no longer has.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leads")
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
    @Column(name = "status", nullable = false, length = 40)
    private LeadStatus status;

    /** No public setter: every write goes through {@link #changeTravelers}, which enforces the lock. */
    @Setter(lombok.AccessLevel.NONE)
    @Embedded
    private TravelerParty travelers;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmed_room_type", length = 20)
    private RoomType confirmedRoomType;

    @Column(name = "confirmed_price", precision = 10, scale = 2)
    private BigDecimal confirmedPrice;

    // --- Pricing snapshot: written once at creation, read-only thereafter ---

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "commission_per_traveler", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionPerTraveler;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "cashback_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal cashbackAmount;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "commission_policy", nullable = false, length = 50)
    private String commissionPolicy;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "cashback_policy", nullable = false, length = 50)
    private String cashbackPolicy;

    // --- Who did what, and when ---

    @Column(name = "contacted_by")
    private UUID contactedBy;

    @Column(name = "contacted_at")
    private Instant contactedAt;

    @Column(name = "deposit_reported_by")
    private UUID depositReportedBy;

    @Column(name = "deposit_reported_at")
    private Instant depositReportedAt;

    @Column(name = "deposit_confirmed_by")
    private UUID depositConfirmedBy;

    @Column(name = "deposit_confirmed_at")
    private Instant depositConfirmedAt;

    @Column(name = "full_payment_reported_by")
    private UUID fullPaymentReportedBy;

    @Column(name = "full_payment_reported_at")
    private Instant fullPaymentReportedAt;

    @Column(name = "full_payment_confirmed_by")
    private UUID fullPaymentConfirmedBy;

    @Column(name = "full_payment_confirmed_at")
    private Instant fullPaymentConfirmedAt;

    @Column(name = "commission_reported_by")
    private UUID commissionReportedBy;

    @Column(name = "commission_reported_at")
    private Instant commissionReportedAt;

    @Column(name = "commission_paid_by")
    private UUID commissionPaidBy;

    @Column(name = "commission_paid_at")
    private Instant commissionPaidAt;

    @Column(name = "cashback_paid_by")
    private UUID cashbackPaidBy;

    @Column(name = "cashback_paid_at")
    private Instant cashbackPaidAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** The billable traveller count the commission was charged on. */
    public int getTravelerCount() {
        return travelers == null ? 0 : travelers.getTravelerCount();
    }

    /**
     * Applies the pricing snapshot. Called exactly once, by the create-lead use case; refusing a
     * second call is what makes "never recalculated after creation" a guarantee rather than a
     * convention.
     */
    public void applyPricing(LeadPricing pricing) {
        if (commissionAmount != null) {
            throw new ConflictException("A lead's commission and cashback are fixed at creation and cannot be repriced");
        }
        this.commissionPerTraveler = pricing.commissionPerTraveler();
        this.commissionAmount = pricing.commissionAmount();
        this.cashbackAmount = pricing.cashbackAmount();
        this.commissionPolicy = pricing.commissionPolicyCode();
        this.cashbackPolicy = pricing.cashbackPolicyCode();
    }

    /**
     * The party can still be corrected while money is still moving; it freezes once the booking is
     * settled in full. Note that editing the counts never reprices the lead — commission and
     * cashback stay as they were fixed at creation.
     *
     * <p>This one constant is the entire cut-off rule. Clients read the resulting flag rather than
     * comparing statuses themselves, so moving it needs no client release.
     */
    private static final LeadStatus TRAVELERS_LOCKED_FROM = LeadStatus.FULLY_PAID;

    /**
     * The terminal check comes first because CANCELLED sits off the status ladder: a stage
     * comparison alone would read it as "earlier than FULLY_PAID" and hand out an edit affordance
     * on a lead the customer has already withdrawn from.
     */
    public boolean areTravelersEditable() {
        return !status.isTerminal() && !status.isAtLeast(TRAVELERS_LOCKED_FROM);
    }

    public void changeTravelers(TravelerParty newTravelers) {
        if (!areTravelersEditable()) {
            throw new ConflictException(status.isCancelled()
                    ? "Traveler counts cannot be changed on a cancelled journey"
                    : "Traveler counts cannot be changed once the booking is paid in full");
        }
        this.travelers = newTravelers;
    }

    /**
     * Stamps the who/when pair belonging to this action. Kept on the entity so the audit trail is a
     * property of the lead itself and cannot be forgotten by a caller that moves the status.
     */
    public void recordAction(LeadAction action, UUID actorUserId, Instant at) {
        switch (action) {
            case MARK_CONTACTED -> {
                contactedBy = actorUserId;
                contactedAt = at;
            }
            case MARK_DEPOSIT_PAID -> {
                depositConfirmedBy = actorUserId;
                depositConfirmedAt = at;
            }
            case MARK_FULLY_PAID -> {
                fullPaymentConfirmedBy = actorUserId;
                fullPaymentConfirmedAt = at;
            }
            case REPORT_COMMISSION_PAID -> {
                commissionReportedBy = actorUserId;
                commissionReportedAt = at;
            }
            case CONFIRM_COMMISSION_PAID -> {
                commissionPaidBy = actorUserId;
                commissionPaidAt = at;
            }
            case PAY_CASHBACK -> {
                cashbackPaidBy = actorUserId;
                cashbackPaidAt = at;
            }
            case CANCEL -> {
                cancelledBy = actorUserId;
                cancelledAt = at;
            }
        }
    }
}
