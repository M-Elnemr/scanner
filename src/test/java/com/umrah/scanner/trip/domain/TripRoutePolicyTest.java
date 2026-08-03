package com.umrah.scanner.trip.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.umrah.scanner.airport.domain.Airport;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.country.domain.Country;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripRoutePolicyTest {

    private static final Country EGYPT = country("Egypt", "EG");
    private static final Country SAUDI = country("Saudi Arabia", "SA");

    private static final Airport CAI = airport("CAI", "Cairo", EGYPT);
    private static final Airport LXR = airport("LXR", "Luxor", EGYPT);
    private static final Airport JED = airport("JED", "Jeddah", SAUDI);
    private static final Airport MED = airport("MED", "Madinah", SAUDI);

    @Test
    void acceptsAStraightThereAndBack() {
        assertThatCode(() -> TripRoutePolicy.validate(CAI, JED, JED, CAI)).doesNotThrowAnyException();
    }

    @Test
    void acceptsAnOpenJawInsideEachCountry() {
        // Fly out to Jeddah, home from Madinah, and land back at a different Egyptian airport.
        // A normal Umrah itinerary, and the case the old single-pair schema could not express.
        assertThatCode(() -> TripRoutePolicy.validate(CAI, JED, MED, LXR)).doesNotThrowAnyException();
    }

    @Test
    void rejectsADomesticOutbound() {
        assertThatThrownBy(() -> TripRoutePolicy.validate(CAI, LXR, LXR, CAI))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must leave the traveler's country");
    }

    @Test
    void rejectsAReturnDepartingTheWrongCountry() {
        // Flies out to Saudi but tries to come back from Egypt.
        assertThatThrownBy(() -> TripRoutePolicy.validate(CAI, JED, LXR, CAI))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must depart from Saudi Arabia");
    }

    @Test
    void rejectsAReturnLandingInTheWrongCountry() {
        assertThatThrownBy(() -> TripRoutePolicy.validate(CAI, JED, MED, JED))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must arrive back in Egypt");
    }

    // --- fixtures ---

    private static Country country(String name, String iso2) {
        Country country = new Country();
        country.setName(name);
        country.setIso2(iso2);
        assignId(country);
        return country;
    }

    private static Airport airport(String iata, String city, Country country) {
        Airport airport = new Airport();
        airport.setIataCode(iata);
        airport.setCity(city);
        airport.setName(city + " International Airport");
        airport.setCountry(country);
        assignId(airport);
        return airport;
    }

    /** BaseEntity's id is JPA-assigned and has no setter; the policy compares by id, so tests must set one. */
    private static void assignId(Object entity) {
        try {
            Field id = Class.forName("com.umrah.scanner.common.domain.BaseEntity").getDeclaredField("id");
            id.setAccessible(true);
            id.set(entity, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
