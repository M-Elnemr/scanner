package com.umrah.scanner.messaging.application;

public enum WhatsAppLanguage {
    AR,
    EN;

    /** Arabic is the default: the customers this composes messages for are Egyptian. */
    public static WhatsAppLanguage fromParam(String lang) {
        return "en".equalsIgnoreCase(lang) ? EN : AR;
    }
}
