package com.umrah.scanner.rating.presentation;

import com.umrah.scanner.rating.domain.Rating;
import java.time.Instant;
import java.util.UUID;

/** Reviewer identity is deliberately not exposed — only the score and comment are public. */
public record RatingResponse(UUID id, short stars, String comment, Instant createdAt) {

    public static RatingResponse from(Rating rating) {
        return new RatingResponse(rating.getId(), rating.getStars(), rating.getComment(), rating.getCreatedAt());
    }
}
