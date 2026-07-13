package com.umc.bscene.domain.chat.dto.response;

import java.util.List;

public record ChatRoomListResponse(
        List<ChatRoomListItemResponse> content,
        int size,
        Long nextCursor,
        boolean hasNext
) {}
