package com.umrah.scanner.pricing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Every monetary amount the platform stores is EGP with 2 decimals, rounded half-up. */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }
}
