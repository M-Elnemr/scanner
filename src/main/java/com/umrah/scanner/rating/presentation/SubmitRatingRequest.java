package com.umrah.scanner.rating.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SubmitRatingRequest(@Min(1) @Max(5) short stars, String comment) {
}
