package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentCreateRequest;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;
import com.umc.bscene.domain.session.enums.code.SessionProfileSuccessCode;
import com.umc.bscene.domain.session.service.SessionRecruitmentCommandService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sessions/recruitments")
public class SessionRecruitmentController {

    private final SessionRecruitmentCommandService sessionRecruitmentCommandService;

    @PostMapping
    public SuccessResponse<SessionRecruitmentCreateResponse> createSessionRecruitment(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody SessionRecruitmentCreateRequest request
    ) {
        Long userId = authMember.getUser().getId();

        SessionRecruitmentCreateResponse response =
                sessionRecruitmentCommandService.createSessionRecruitment(userId, request);

        return SuccessResponse.of(
                response,
                SessionProfileSuccessCode.SESSION_RECRUITMENT_CREATE_SUCCESS
        );
    }
}