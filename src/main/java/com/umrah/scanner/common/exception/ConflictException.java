package com.umrah.scanner.common.exception;

import java.util.Map;

/** The request conflicts with the current state of a resource — maps to HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }

    protected ConflictException(String message, String code, Map<String, Object> properties) {
        super(message, code, properties);
    }
}
