package com.umrah.scanner.trip.domain;

import com.umrah.scanner.common.exception.ValidationException;

public enum RoomType {
    SINGLE,
    DOUBLE,
    TRIPLE,
    QUAD,
    QUINT,
    CHILD,
    INFANT;

    /**
     * Maps the customer-facing "room size" (how many people the room sleeps) to a {@link RoomType}.
     * {@code null} passes through as {@code null} (callers default that to {@link #QUAD}, the same
     * room type "price starts from" is based on) — shared by both the public browse endpoint and the
     * admin trip console so the two never drift on what a given size means.
     */
    public static RoomType forSize(Integer roomSize) {
        if (roomSize == null) {
            return null;
        }
        return switch (roomSize) {
            case 1 -> SINGLE;
            case 2 -> DOUBLE;
            case 3 -> TRIPLE;
            case 4 -> QUAD;
            case 5 -> QUINT;
            default -> throw new ValidationException("roomSize must be 1, 2, 3, 4, or 5");
        };
    }
}
