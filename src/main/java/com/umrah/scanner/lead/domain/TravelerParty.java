package com.umrah.scanner.lead.domain;

import com.umrah.scanner.common.exception.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Who is travelling on a booking request. A value object rather than three loose columns so the
 * invariants (at least one adult, no negative counts) can only ever be satisfied one way, wherever
 * the counts are set from.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelerParty {

    @Column(name = "adult_count", nullable = false)
    private int adultCount;

    @Column(name = "child_count", nullable = false)
    private int childCount;

    @Column(name = "infant_count", nullable = false)
    private int infantCount;

    private TravelerParty(int adultCount, int childCount, int infantCount) {
        this.adultCount = adultCount;
        this.childCount = childCount;
        this.infantCount = infantCount;
    }

    public static TravelerParty of(int adultCount, int childCount, int infantCount) {
        if (adultCount < 1) {
            throw new ValidationException("At least one adult traveler is required");
        }
        if (childCount < 0) {
            throw new ValidationException("Child count cannot be negative");
        }
        if (infantCount < 0) {
            throw new ValidationException("Infant count cannot be negative");
        }
        return new TravelerParty(adultCount, childCount, infantCount);
    }

    /**
     * The billable traveller count — the figure commission is charged on. Children and infants ride
     * along free of commission, so this is the adult count.
     */
    public int getTravelerCount() {
        return adultCount;
    }

    /** Everyone on the booking, including children and infants. Display only — never priced on. */
    public int getTotalHeadCount() {
        return adultCount + childCount + infantCount;
    }
}
