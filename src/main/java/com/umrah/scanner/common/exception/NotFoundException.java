package com.umrah.scanner.common.exception;

/** No matching record — maps to HTTP 404 once the presentation layer's exception handler is wired. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entityName, Object id) {
        return new NotFoundException(entityName + " not found: " + id);
    }
}
