package com.umrah.scanner.messaging.application;

import com.umrah.scanner.common.exception.ValidationException;

/**
 * Turns whatever a customer or company typed into their phone field into the bare-digits,
 * country-code-prefixed form {@code wa.me} requires — no {@code +}, no spaces, no leading zero.
 *
 * <p>{@code customer_profiles.phone} and {@code company_profiles.whatsapp}/
 * {@code company_addresses.mobile_number} are all unvalidated free text (no {@code @Pattern}
 * anywhere on them), so this is the one place that has to make sense of whatever shape they're in.
 * Handles: a leading {@code +}, a leading {@code 00} international prefix, a local Egyptian mobile
 * typed with its domestic leading zero (11 digits, e.g. {@code 01012345678}), and embedded spaces
 * or dashes. Anything it cannot make sense of is a {@link ValidationException} rather than a
 * silently broken link.
 */
public final class PhoneNumbers {

    private static final String EGYPT_COUNTRY_CODE = "20";

    private PhoneNumbers() {
    }

    public static String toWhatsAppDigits(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new ValidationException("No phone number is on file for this recipient");
        }

        String digits = rawPhone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        } else if (digits.startsWith("00")) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() == 11) {
            // A local Egyptian mobile (e.g. 01012345678) — drop the domestic leading zero and add
            // the country code, rather than the '00'/'+' international prefix nobody dials locally.
            digits = EGYPT_COUNTRY_CODE + digits.substring(1);
        }

        if (digits.length() < 10 || digits.length() > 15 || !digits.matches("[0-9]+")) {
            throw new ValidationException("\"" + rawPhone + "\" is not a valid WhatsApp number");
        }
        return digits;
    }

    public static String toWhatsAppLink(String rawPhone, String message) {
        String digits = toWhatsAppDigits(rawPhone);
        return "https://wa.me/" + digits + "?text=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
    }
}
