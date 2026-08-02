package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadAction;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.domain.LeadTransitionPolicy;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.user.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * A lead as one particular caller is allowed to see it.
 *
 * <p>The projection is role-aware on purpose: commission is what the <em>company</em> owes the
 * platform and is never shown to a customer, who only ever sees the cashback figure. Build these
 * through {@link #forRole} rather than a single all-fields factory, so a new endpoint cannot
 * accidentally leak the commission by picking the wrong mapper.
 */
@Schema(name = "Lead", description = "A booking request and its lifecycle. Commission fields are omitted for customers.")
public record LeadResponse(
        UUID id,
        UUID customerId,
        UUID tripId,
        String tripTitle,
        UUID companyId,
        String companyName,
        @Schema(description = "Who is travelling. The company needs this to serve the booking; it never includes the payout wallet.")
        LeadCustomer customer,
        LeadStatus status,

        @Schema(description = "Adults on the booking; always at least 1") int adultCount,
        int childCount,
        int infantCount,
        @Schema(description = "Billable travelers — the count commission and cashback were calculated on") int travelerCount,
        @Schema(description = "Whether the traveler counts can still be changed. Gate the edit affordance on this "
                + "flag rather than on the status, so a future change to the cut-off needs no client release.")
        boolean travelersEditable,

        @Schema(description = "EGP the customer receives after the company settles its commission") BigDecimal cashbackAmount,
        @Schema(description = "EGP the company owes the platform. Null for customers.") BigDecimal commissionAmount,
        @Schema(description = "The company's per-traveler rate at the time this lead was created. Null for customers.")
        BigDecimal commissionPerTraveler,

        RoomType confirmedRoomType,
        BigDecimal confirmedPrice,

        @Schema(description = "Exactly the actions this caller may perform on this lead right now")
        Set<LeadAction> availableActions,
        LeadAudit audit,

        Instant createdAt,
        Instant updatedAt) {

    /**
     * The contact details behind a lead. Populated for all three roles: the company needs them to
     * serve the booking the customer has explicitly opened with them, and the customer is simply
     * seeing their own data back.
     *
     * <p>{@code cashbackWalletNumber} and {@code walletType} are deliberately absent — the payout
     * wallet is between the customer and the platform, and a company has no reason to see it.
     */
    @Schema(name = "LeadCustomer", description = "Contact details for the traveller who opened the lead.")
    public record LeadCustomer(UUID id, String fullName, String phone) {

        static LeadCustomer from(CustomerProfile customer) {
            return new LeadCustomer(customer.getId(), customer.getFullName(), customer.getPhone());
        }
    }

    @Schema(name = "LeadAudit", description = "Who reported and confirmed each payment step, and when.")
    public record LeadAudit(
            UUID depositReportedBy,
            Instant depositReportedAt,
            UUID depositConfirmedBy,
            Instant depositConfirmedAt,
            UUID fullPaymentReportedBy,
            Instant fullPaymentReportedAt,
            UUID fullPaymentConfirmedBy,
            Instant fullPaymentConfirmedAt,
            UUID commissionReportedBy,
            Instant commissionReportedAt,
            UUID commissionPaidBy,
            Instant commissionPaidAt,
            UUID cashbackPaidBy,
            Instant cashbackPaidAt) {

        static LeadAudit from(Lead lead) {
            return new LeadAudit(
                    lead.getDepositReportedBy(), lead.getDepositReportedAt(),
                    lead.getDepositConfirmedBy(), lead.getDepositConfirmedAt(),
                    lead.getFullPaymentReportedBy(), lead.getFullPaymentReportedAt(),
                    lead.getFullPaymentConfirmedBy(), lead.getFullPaymentConfirmedAt(),
                    lead.getCommissionReportedBy(), lead.getCommissionReportedAt(),
                    lead.getCommissionPaidBy(), lead.getCommissionPaidAt(),
                    lead.getCashbackPaidBy(), lead.getCashbackPaidAt());
        }
    }

    /** Customers see cashback and traveler counts, never commission. */
    public static LeadResponse forCustomer(Lead lead) {
        return build(lead, Role.CUSTOMER, false);
    }

    /** The company sees what it owes the platform. */
    public static LeadResponse forCompany(Lead lead) {
        return build(lead, Role.COMPANY, true);
    }

    public static LeadResponse forAdmin(Lead lead) {
        return build(lead, Role.ADMIN, true);
    }

    public static LeadResponse forRole(Lead lead, Role role) {
        return switch (role) {
            case CUSTOMER -> forCustomer(lead);
            case COMPANY -> forCompany(lead);
            case ADMIN -> forAdmin(lead);
        };
    }

    private static LeadResponse build(Lead lead, Role role, boolean includeCommission) {
        var travelers = lead.getTravelers();
        return new LeadResponse(
                lead.getId(),
                lead.getCustomer().getId(),
                lead.getTrip().getId(),
                lead.getTrip().getTitle(),
                lead.getCompany().getId(),
                lead.getCompany().getCompanyName(),
                LeadCustomer.from(lead.getCustomer()),
                lead.getStatus(),
                travelers.getAdultCount(),
                travelers.getChildCount(),
                travelers.getInfantCount(),
                lead.getTravelerCount(),
                lead.areTravelersEditable(),
                lead.getCashbackAmount(),
                includeCommission ? lead.getCommissionAmount() : null,
                includeCommission ? lead.getCommissionPerTraveler() : null,
                lead.getConfirmedRoomType(),
                lead.getConfirmedPrice(),
                LeadTransitionPolicy.availableActions(lead.getStatus(), role),
                LeadAudit.from(lead),
                lead.getCreatedAt(),
                lead.getUpdatedAt());
    }
}
