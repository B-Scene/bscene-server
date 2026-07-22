package com.umc.bscene.domain.chat.controller;

import com.umc.bscene.domain.chat.dto.response.ChatWebSocketTicketResponse;
import com.umc.bscene.domain.chat.enums.code.success.ChatSuccessCode;
import com.umc.bscene.domain.chat.service.ChatWebSocketTicketService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatWebSocketTicketController {
    private final ChatWebSocketTicketService ticketService;

    @PostMapping("/ws-ticket")
    public SuccessResponse<ChatWebSocketTicketResponse> issueTicket(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        return SuccessResponse.of(
                ticketService.issue(authMember.getUser().getId()),
                ChatSuccessCode.DM_TICKET_ISSUE_SUCCESS
        );
    }
}
