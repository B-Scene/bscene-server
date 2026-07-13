package com.umc.bscene.domain.chat.controller;

import com.umc.bscene.domain.chat.dto.request.ChatRoomCreateRequest;
import com.umc.bscene.domain.chat.dto.response.ChatRoomCreateResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomListResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomDetailResponse;
import com.umc.bscene.domain.chat.enums.ChatRoomFilter;
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

    @GetMapping
    public SuccessResponse<ChatRoomListResponse> getChatRooms(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "ALL") ChatRoomFilter filter,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return SuccessResponse.of(
                chatRoomService.getMyRooms(
                        authMember.getUser().getId(), filter, cursorId, size),
                ChatSuccessCode.CHAT_ROOM_LIST_SUCCESS
        );
    }

    @GetMapping("/{chatRoomId}")
    public SuccessResponse<ChatRoomDetailResponse> getChatRoomDetail(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long chatRoomId
    ) {
        return SuccessResponse.of(
                chatRoomService.getRoomDetail(
                        authMember.getUser().getId(), chatRoomId),
                ChatSuccessCode.CHAT_ROOM_DETAIL_SUCCESS
        );
    }

    @DeleteMapping("/{chatRoomId}")
    public SuccessResponse<Void> leaveChatRoom(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long chatRoomId
    ) {
        chatRoomService.leaveRoom(authMember.getUser().getId(), chatRoomId);
        return new SuccessResponse<>(null, ChatSuccessCode.CHAT_ROOM_LEAVE_SUCCESS);
    }
}
