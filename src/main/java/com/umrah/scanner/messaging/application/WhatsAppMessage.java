package com.umrah.scanner.messaging.application;

/**
 * @param link a {@code wa.me} click-to-chat URL with {@code message} pre-filled. Opening it starts
 *             a WhatsApp chat with the text already typed in — the admin still has to press send.
 *             No Cloud API, no access token, no per-message cost.
 */
public record WhatsAppMessage(String recipientName, String recipientPhone, String message, String link) {
}
