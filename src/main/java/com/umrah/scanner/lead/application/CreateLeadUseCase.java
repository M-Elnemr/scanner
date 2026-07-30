package com.umrah.scanner.lead.application;

import com.umrah.scanner.analytics.application.AnalyticsEventService;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.domain.LeadStatusHistory;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.lead.infrastructure.LeadStatusHistoryRepository;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Contact company." Idempotent by design — re-contacting a trip returns the existing
 * lead instead of erroring, matching a mobile client that may retry the request.
 */
@Service
public class CreateLeadUseCase {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository leadStatusHistoryRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TripRepository tripRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AnalyticsEventService analyticsEventService;

    public CreateLeadUseCase(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository leadStatusHistoryRepository,
            CustomerProfileRepository customerProfileRepository,
            TripRepository tripRepository,
            NotificationDispatcher notificationDispatcher,
            AnalyticsEventService analyticsEventService) {
        this.leadRepository = leadRepository;
        this.leadStatusHistoryRepository = leadStatusHistoryRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.tripRepository = tripRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.analyticsEventService = analyticsEventService;
    }

    @Transactional
    public Lead execute(UUID customerUserId, UUID tripId) {
        CustomerProfile customer = customerProfileRepository.findByUserId(customerUserId)
                .orElseThrow(() -> new ValidationException("Complete your profile before contacting a company"));
        if (!customer.isProfileCompleted()) {
            throw new ValidationException("Complete your profile before contacting a company");
        }

        Trip trip = tripRepository.findWithDetailsById(tripId).orElseThrow(() -> NotFoundException.of("Trip", tripId));
        if (trip.getStatus() != TripStatus.PUBLISHED) {
            throw new ValidationException("This trip is not available");
        }

        return leadRepository.findByCustomerIdAndTripId(customer.getId(), tripId)
                .orElseGet(() -> createLead(customer, trip, customerUserId));
    }

    private Lead createLead(CustomerProfile customer, Trip trip, UUID actingUserId) {
        Lead lead = new Lead();
        lead.setCustomer(customer);
        lead.setTrip(trip);
        lead.setCompany(trip.getCompany());
        lead.setStatus(LeadStatus.INTERESTED);
        lead = leadRepository.save(lead);

        LeadStatusHistory history = new LeadStatusHistory();
        history.setLead(lead);
        history.setToStatus(LeadStatus.INTERESTED);
        history.setChangedBy(actingUserId);
        leadStatusHistoryRepository.save(history);

        notificationDispatcher.dispatch(
                trip.getCompany().getUser().getId(),
                "NEW_LEAD",
                "New interested customer",
                "A customer is interested in " + trip.getTitle(),
                Map.of("leadId", lead.getId().toString(), "tripId", trip.getId().toString()));

        analyticsEventService.record(actingUserId, "CONTACT_COMPANY", "Trip", trip.getId(), null);

        return lead;
    }
}
