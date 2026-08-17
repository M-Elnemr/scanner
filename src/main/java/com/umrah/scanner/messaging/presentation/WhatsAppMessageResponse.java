package com.umrah.scanner.messaging.presentation;

import com.umrah.scanner.messaging.application.WhatsAppMessage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WhatsAppMessage", description = "A composed message and a wa.me click-to-chat link ready to send.")
public record WhatsAppMessageResponse(
        String recipientName,
        String recipientPhone,
        String message,
        @Schema(description = "Opens WhatsApp with `message` pre-filled — the admin still presses send")
        String link) {

    public static WhatsAppMessageResponse from(WhatsAppMessage message) {
        return new WhatsAppMessageResponse(message.recipientName(), message.recipientPhone(), message.message(), message.link());
    }
}
