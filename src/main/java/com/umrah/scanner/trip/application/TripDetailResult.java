package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.Trip;
import java.math.BigDecimal;

/**
 * @param companyVisible      true only once the viewing customer has an active lead on this trip
 * @param cashbackPerTraveler EGP a single traveler earns back — the only pricing figure a customer
 *                            is shown. The commission it derives from is never exposed.
 * @param includeCommission   true for the company/admin-owned reads, false for the public/customer
 *                            one — same split as {@code LeadResponse}'s own includeCommission flag.
 */
public record TripDetailResult(Trip trip, boolean companyVisible, BigDecimal cashbackPerTraveler, boolean includeCommission) {
}
