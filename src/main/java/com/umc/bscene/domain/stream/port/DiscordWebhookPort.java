package com.umc.bscene.domain.stream.port;

import com.umc.bscene.domain.stream.dto.request.DiscordWebhookRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface DiscordWebhookPort {

    @PostExchange
    void send(@RequestBody DiscordWebhookRequest request);
}
