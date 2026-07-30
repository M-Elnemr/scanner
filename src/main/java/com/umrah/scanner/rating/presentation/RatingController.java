package com.umrah.scanner.rating.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.rating.application.RatingQueryService;
import com.umrah.scanner.rating.application.SubmitRatingUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RatingController {

    private final SubmitRatingUseCase submitRatingUseCase;
    private final RatingQueryService ratingQueryService;

    public RatingController(SubmitRatingUseCase submitRatingUseCase, RatingQueryService ratingQueryService) {
        this.submitRatingUseCase = submitRatingUseCase;
        this.ratingQueryService = ratingQueryService;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/leads/{id}/rating")
    public ApiResponse<RatingResponse> submit(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id, @Valid @RequestBody SubmitRatingRequest request) {
        var rating = submitRatingUseCase.execute(currentUser.userId(), id, request.stars(), request.comment());
        return ApiResponse.of(RatingResponse.from(rating));
    }

    @GetMapping("/api/v1/trips/{id}/ratings")
    public ApiResponse<PageResponse<RatingResponse>> listForTrip(@PathVariable UUID id, Pageable pageable) {
        return ApiResponse.of(PageResponse.of(ratingQueryService.listForTrip(id, pageable), RatingResponse::from));
    }

    @GetMapping("/api/v1/companies/{id}/ratings")
    public ApiResponse<PageResponse<RatingResponse>> listForCompany(@PathVariable UUID id, Pageable pageable) {
        return ApiResponse.of(PageResponse.of(ratingQueryService.listForCompany(id, pageable), RatingResponse::from));
    }
}
