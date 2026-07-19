package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.DiscordWebhookRequest;
import com.umc.bscene.domain.stream.port.DiscordWebhookPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DiscordMessageSender {

    private final DiscordWebhookPort discordWebhookPort;

    public void send(DiscordWebhookRequest request) {
        discordWebhookPort.send(request);
    }

}
