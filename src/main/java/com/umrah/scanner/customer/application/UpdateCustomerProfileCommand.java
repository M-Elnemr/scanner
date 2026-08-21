package com.umrah.scanner.customer.application;

public record UpdateCustomerProfileCommand(
        String fullName,
        String phone) {
}
