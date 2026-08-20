package com.umrah.scanner.messaging.application;

import com.umrah.scanner.company.application.CompanyQueryService;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.lead.application.LeadQueryService;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.trip.application.TripQueryService;
import com.umrah.scanner.trip.domain.Trip;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the admin dashboard's two WhatsApp buttons on a lead: one message with the trip details,
 * one with the company's. Both go to the customer behind the lead — composing a message for anyone
 * else is out of scope, since these exist to make first contact easy, not as a general messaging
 * tool.
 */
@Service
public class WhatsAppLeadMessageService {

    private final LeadQueryService leadQueryService;
    private final TripQueryService tripQueryService;
    private final CompanyQueryService companyQueryService;
    private final WhatsAppMessageComposer composer;

    public WhatsAppLeadMessageService(
            LeadQueryService leadQueryService,
            TripQueryService tripQueryService,
            CompanyQueryService companyQueryService,
            WhatsAppMessageComposer composer) {
        this.leadQueryService = leadQueryService;
        this.tripQueryService = tripQueryService;
        this.companyQueryService = companyQueryService;
        this.composer = composer;
    }

    @Transactional(readOnly = true)
    public WhatsAppMessage tripMessage(UUID leadId, WhatsAppLanguage lang) {
        Lead lead = leadQueryService.getForAdmin(leadId);
        Trip trip = tripQueryService.ownedDetail(lead.getTripId()).trip();
        String message = composer.composeTripMessage(
                trip,
                lead.getCustomer().getFullName(),
                lead.getTravelers().getAdultCount(),
                lead.getTravelers().getChildCount(),
                lead.getTravelers().getInfantCount(),
                lang);
        return toMessage(lead, message);
    }

    @Transactional(readOnly = true)
    public WhatsAppMessage companyMessage(UUID leadId, WhatsAppLanguage lang) {
        Lead lead = leadQueryService.getForAdmin(leadId);
        CompanyProfile company = companyQueryService.getById(lead.getCompanyId());
        String message = composer.composeCompanyMessage(company, lead.getCustomer().getFullName(), lang);
        return toMessage(lead, message);
    }

    private WhatsAppMessage toMessage(Lead lead, String message) {
        String phone = lead.getCustomer().getPhone();
        String link = PhoneNumbers.toWhatsAppLink(phone, message);
        return new WhatsAppMessage(lead.getCustomer().getFullName(), phone, message, link);
    }
}
