package com.umrah.scanner.lead.application;

import java.util.UUID;

/** "Contact this company about this trip, for this many travelers." */
public record CreateLeadCommand(UUID tripId, int adultCount, int childCount, int infantCount) {
}
