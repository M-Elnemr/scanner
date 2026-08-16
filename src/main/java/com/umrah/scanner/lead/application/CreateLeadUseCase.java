package com.umrah.scanner.lead.application;

import com.umrah.scanner.analytics.application.AnalyticsEventService;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.domain.LeadStatusHistory;
import com.umrah.scanner.lead.domain.TravelerParty;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.lead.infrastructure.LeadStatusHistoryRepository;
import com.umrah.scanner.pricing.application.LeadPricingService;
import com.umrah.scanner.pricing.domain.PricingRequest;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Contact company." The single point at which a lead's commission and cashback are ever computed —
 * both are snapshotted here and are read-only for the rest of the lead's life.
 *
 * <p>Idempotent by design: re-contacting a trip returns the existing lead instead of erroring,
 * matching a mobile client that may retry. If the traveller counts differ and the lead has not been
 * locked yet, the counts are updated — but the original pricing stands, exactly as the rules require.
 */
@Service
public class CreateLeadUseCase {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository leadStatusHistoryRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TripRepository tripRepository;
    private final LeadPricingService leadPricingService;
    private final LeadNotifier leadNotifier;
    private final AnalyticsEventService analyticsEventService;

    public CreateLeadUseCase(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository leadStatusHistoryRepository,
            CustomerProfileRepository customerProfileRepository,
            TripRepository tripRepository,
            LeadPricingService leadPricingService,
            LeadNotifier leadNotifier,
            AnalyticsEventService analyticsEventService) {
        this.leadRepository = leadRepository;
        this.leadStatusHistoryRepository = leadStatusHistoryRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.tripRepository = tripRepository;
        this.leadPricingService = leadPricingService;
        this.leadNotifier = leadNotifier;
        this.analyticsEventService = analyticsEventService;
    }

    @Transactional
    public Lead execute(UUID customerUserId, CreateLeadCommand command) {
        CustomerProfile customer = customerProfileRepository.findByUserId(customerUserId)
                .orElseThrow(() -> new ValidationException("Complete your profile before contacting a company"));
        if (!customer.isProfileCompleted()) {
            throw new ValidationException("Complete your profile before contacting a company");
        }

        Trip trip = tripRepository.findWithDetailsById(command.tripId())
                .orElseThrow(() -> NotFoundException.of("Trip", command.tripId()));
        if (trip.getStatus() != TripStatus.PUBLISHED) {
            throw new ValidationException("This trip is not available");
        }

        TravelerParty travelers = TravelerParty.of(command.adultCount(), command.childCount(), command.infantCount());

        requireNoOtherActiveLead(customer.getId(), command.tripId());

        return leadRepository.findNotCancelledByCustomerIdAndTripId(customer.getId(), command.tripId())
                .map(existing -> resumeExisting(existing, travelers))
                .orElseGet(() -> createLead(customer, trip, travelers, customerUserId));
    }

    /**
     * A customer preserves one journey at a time: to take up a different trip they must cancel the
     * one they are holding. Their own trip is exempt, which is what keeps re-submitting the same
     * trip idempotent rather than turning a retry into a conflict.
     *
     * <p>The database enforces the same rule through the {@code uq_leads_customer_active} partial
     * index; this check exists so the client gets a 409 naming the trip in the way, instead of an
     * opaque constraint violation.
     */
    private void requireNoOtherActiveLead(UUID customerId, UUID tripId) {
        leadRepository.findActiveByCustomerId(customerId)
                .filter(active -> !active.getTrip().getId().equals(tripId))
                .ifPresent(active -> {
                    throw new ActiveLeadExistsException(active);
                });
    }

    /** A retry, or a customer re-opening the same trip: same lead, same price, counts refreshed if still open. */
    private Lead resumeExisting(Lead lead, TravelerParty travelers) {
        if (lead.areTravelersEditable()) {
            lead.changeTravelers(travelers);
        }
        return lead;
    }

    private Lead createLead(CustomerProfile customer, Trip trip, TravelerParty travelers, UUID actingUserId) {
        Lead lead = new Lead();
        lead.setCustomer(customer);
        lead.setTrip(trip);
        lead.setCompany(trip.getCompany());
        lead.setStatus(LeadStatus.INTERESTED);
        lead.changeTravelers(travelers);
        lead.applyPricing(leadPricingService.price(new PricingRequest(
                trip.getCompany().getId(),
                trip.getId(),
                customer.getId(),
                trip.getCompany().getCommissionPerTraveler(),
                travelers.getAdultCount(),
                travelers.getChildCount(),
                travelers.getInfantCount(),
                travelers.getTravelerCount(),
                Instant.now())));
        lead = leadRepository.save(lead);

        LeadStatusHistory history = new LeadStatusHistory();
        history.setLead(lead);
        history.setToStatus(LeadStatus.INTERESTED);
        history.setChangedBy(actingUserId);
        leadStatusHistoryRepository.save(history);

        leadNotifier.leadCreated(lead);
        analyticsEventService.record(actingUserId, "CONTACT_COMPANY", "Trip", trip.getId(), null);

        return lead;
    }
}
