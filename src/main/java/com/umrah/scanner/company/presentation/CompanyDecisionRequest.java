package com.umrah.scanner.company.presentation;

import jakarta.validation.constraints.NotBlank;

/** Body for admin reject/suspend actions — both require a human-readable reason. */
public record CompanyDecisionRequest(@NotBlank String reason) {
}
