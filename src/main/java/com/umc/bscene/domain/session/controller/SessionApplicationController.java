package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.enums.code.SessionSuccessCode;
import com.umc.bscene.domain.session.service.SessionApplicationCommandService;
import com.umc.bscene.domain.session.service.SessionApplicationQueryService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sessions/applications")
public class SessionApplicationController {

    private final SessionApplicationQueryService sessionApplicationQueryService;
    private final SessionApplicationCommandService sessionApplicationCommandService;

    @GetMapping
    public SuccessResponse<List<MySessionApplicationResponse>> getMySessionApplications(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long userId = authMember.getUser().getId();

        List<MySessionApplicationResponse> response =
                sessionApplicationQueryService.getMySessionApplications(userId);

        if (response.isEmpty()) {
            return SuccessResponse.of(
                    response,
                    SessionSuccessCode.MY_SESSION_APPLICATION_EMPTY
            );
        }

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_SESSION_APPLICATION_GET_SUCCESS
        );
    }

    @PostMapping
    public SuccessResponse<MySessionApplicationResponse> createSessionApplication(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MySessionApplicationUpdateRequest request
    ) {
        Long userId = authMember.getUser().getId();

        MySessionApplicationResponse response =
                sessionApplicationCommandService.createSessionApplication(userId, request);

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_SESSION_APPLICATION_CREATE_SUCCESS
        );
    }

    @PatchMapping("/{sessionApplicationId}")
    public SuccessResponse<MySessionApplicationResponse> updateSessionApplication(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionApplicationId,
            @Valid @RequestBody MySessionApplicationUpdateRequest request
    ) {
        Long userId = authMember.getUser().getId();

        MySessionApplicationResponse response =
                sessionApplicationCommandService.updateSessionApplication(
                        userId,
                        sessionApplicationId,
                        request
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_SESSION_APPLICATION_UPDATE_SUCCESS
        );
    }
}
