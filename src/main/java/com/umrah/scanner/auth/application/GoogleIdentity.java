package com.umrah.scanner.auth.application;

/** The verified claims lifted out of a Google ID token. */
public record GoogleIdentity(String subject, String email) {
}
