package com.umrah.scanner.auth.application;

/**
 * Hashes a raw, high-entropy refresh token for storage. Unlike a password hasher, this must be
 * fast and deterministic — refresh tokens are looked up by exact hash match, not verified one at a time.
 */
public interface TokenHasher {

    String hash(String rawToken);
}
