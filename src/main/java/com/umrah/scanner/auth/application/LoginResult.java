package com.umrah.scanner.auth.application;

import com.umrah.scanner.user.domain.User;

public record LoginResult(User user, TokenPair tokens) {
}
