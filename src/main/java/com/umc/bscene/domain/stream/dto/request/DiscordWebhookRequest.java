package com.umc.bscene.domain.stream.dto.request;

import com.umc.bscene.domain.stream.enums.DiscordEventMessage;

public record DiscordWebhookRequest(
        String content
) {

    public static DiscordWebhookRequest of(DiscordEventMessage message, Object... args) {
        return new DiscordWebhookRequest(message.getContent().formatted(args));
    }
}
