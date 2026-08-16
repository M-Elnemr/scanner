package com.umrah.scanner.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base for the errors this API raises deliberately, as opposed to the ones it suffers.
 *
 * <p>A subclass may carry a machine-readable {@code code} and a bag of extra properties.
 * {@code GlobalExceptionHandler} copies both onto the problem+json body, which is what lets a
 * client branch on the failure — "you already have a preserved journey, here is which one" — rather
 * than pattern-matching an English sentence that is free to change.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final Map<String, Object> properties;

    protected DomainException(String message) {
        this(message, null, Map.of());
    }

    protected DomainException(String message, String code, Map<String, Object> properties) {
        super(message);
        this.code = code;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    /** A stable identifier for this failure, or null when the message is the whole story. */
    public String code() {
        return code;
    }

    /** Extra fields to surface on the problem+json body. Never null. */
    public Map<String, Object> properties() {
        return properties;
    }
}
