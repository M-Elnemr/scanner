package com.umrah.scanner.company.application;

/** Everything {@link UpdateCompanyProfileCommand} carries, plus the licence number an admin may correct. */
public record AdminUpdateCompanyProfileCommand(String licenseNumber, UpdateCompanyProfileCommand profile) {
}
