package com.umrah.scanner.analytics.application;

/**
 * Event counts split by whether the event carried a signed-in {@code userId}, plus the distinct
 * signed-in user count — {@code identifiedEvents} alone conflates one user firing many events with
 * many users firing one each, which {@code uniqueSignedInUsers} disambiguates. There is no
 * equivalent distinct count for guests: a guest event carries no stable identifier at all (see
 * {@code AnalyticsEvent.userId}'s own "nullable for anonymous browsing" comment).
 */
public record AudienceSplit(long guestEvents, long identifiedEvents, long uniqueSignedInUsers) {
}
