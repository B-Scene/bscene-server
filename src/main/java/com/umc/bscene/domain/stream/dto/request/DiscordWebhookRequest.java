package com.umc.bscene.domain.stream.dto.request;

import com.umc.bscene.domain.stream.enums.DiscordEventMessage;

public record DiscordWebhookRequest(
        DiscordEventMessage message
) {
}
