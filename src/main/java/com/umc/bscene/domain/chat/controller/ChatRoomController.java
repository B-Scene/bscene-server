package com.umc.bscene.domain.chat.controller;

import com.umc.bscene.domain.chat.dto.request.ChatRoomCreateRequest;
import com.umc.bscene.domain.chat.dto.response.ChatRoomCreateResponse;
import com.umc.bscene.domain.chat.response.code.ChatSuccessCode;
import com.umc.bscene.domain.chat.service.ChatRoomService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping
    public SuccessResponse<ChatRoomCreateResponse> createChatRoom(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody ChatRoomCreateRequest request
    ) {
        return SuccessResponse.of(
                chatRoomService.createOrGet(authMember.getUser().getId(), request),
                ChatSuccessCode.CHAT_ROOM_CREATE_SUCCESS
        );
    }
}
