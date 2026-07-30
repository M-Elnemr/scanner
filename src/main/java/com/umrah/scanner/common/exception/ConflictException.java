package com.umrah.scanner.common.exception;

/** The request conflicts with the current state of a resource — maps to HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
