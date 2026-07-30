package com.umrah.scanner.rating.application;

import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.ForbiddenException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.rating.domain.Rating;
import com.umrah.scanner.rating.infrastructure.RatingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitRatingUseCase {

    private final RatingRepository ratingRepository;
    private final LeadRepository leadRepository;
    private final CompanyProfileRepository companyProfileRepository;

    public SubmitRatingUseCase(
            RatingRepository ratingRepository,
            LeadRepository leadRepository,
            CompanyProfileRepository companyProfileRepository) {
        this.ratingRepository = ratingRepository;
        this.leadRepository = leadRepository;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Transactional
    public Rating execute(UUID customerUserId, UUID leadId, short stars, String comment) {
        if (stars < 1 || stars > 5) {
            throw new ValidationException("Stars must be between 1 and 5");
        }
        Lead lead = leadRepository.findById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        if (!lead.getCustomer().getUser().getId().equals(customerUserId)) {
            throw new ForbiddenException("This lead does not belong to the caller");
        }
        if (lead.getStatus() != LeadStatus.CASHBACK_SENT) {
            throw new ValidationException("Rating is only available once the trip is complete");
        }
        if (ratingRepository.existsByLeadId(leadId)) {
            throw new ConflictException("This lead has already been rated");
        }

        Rating rating = new Rating();
        rating.setLead(lead);
        rating.setTrip(lead.getTrip());
        rating.setCompany(lead.getCompany());
        rating.setCustomer(lead.getCustomer());
        rating.setStars(stars);
        rating.setComment(comment);
        rating = ratingRepository.save(rating);

        recomputeCompanyRating(lead.getCompany().getId());
        return rating;
    }

    private void recomputeCompanyRating(UUID companyId) {
        long count = ratingRepository.countByCompanyId(companyId);
        Double average = ratingRepository.avgStarsByCompanyId(companyId);

        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));
        company.setRatingCount((int) count);
        company.setRatingAvg(average == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
    }
}
