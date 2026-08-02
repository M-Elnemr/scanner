package com.umrah.scanner.company.application;

import java.util.UUID;

public record CompanyAddressInput(UUID cityId, String addressText, String mobileNumber) {
}
