package com.umc.bscene.domain.chat.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

public record ChatWebSocketFrame(
        String type,
        JsonNode data,
        String clientMsgId
) {
}
