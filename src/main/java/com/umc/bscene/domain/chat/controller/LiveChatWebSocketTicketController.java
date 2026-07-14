package com.umc.bscene.domain.chat.controller;

import com.umc.bscene.domain.chat.dto.response.LiveChatWebSocketTicketResponse;
import com.umc.bscene.domain.chat.response.code.LiveChatWebSocketSuccessCode;
import com.umc.bscene.domain.chat.service.LiveChatWebSocketTicketService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/lives/{liveId}/chat")
public class LiveChatWebSocketTicketController {
    private final LiveChatWebSocketTicketService ticketService;

    @PostMapping("/ws-ticket")
    public SuccessResponse<LiveChatWebSocketTicketResponse> issueTicket(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        return SuccessResponse.of(
                ticketService.issue(authMember.getUser().getId(), liveId),
                LiveChatWebSocketSuccessCode.TICKET_ISSUE_SUCCESS
        );
    }
}
