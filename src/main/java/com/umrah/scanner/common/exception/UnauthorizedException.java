package com.umrah.scanner.common.exception;

/** Caller could not be authenticated at all (missing/invalid/expired credentials) — maps to HTTP 401. */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
