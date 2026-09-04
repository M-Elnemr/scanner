package com.umrah.scanner.analytics.application;

/** Event counts split by whether the event carried a signed-in {@code userId}. */
public record AudienceSplit(long guestEvents, long identifiedEvents) {
}
