package com.umrah.scanner.common.exception;

/** Caller is authenticated but not entitled to act on this resource — maps to HTTP 403. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }
}
