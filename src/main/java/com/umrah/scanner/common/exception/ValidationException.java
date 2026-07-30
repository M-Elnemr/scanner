package com.umrah.scanner.common.exception;

/** A business-rule invariant was violated — maps to HTTP 422. Distinct from Bean Validation, which rejects malformed input before a use case ever runs. */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }
}
