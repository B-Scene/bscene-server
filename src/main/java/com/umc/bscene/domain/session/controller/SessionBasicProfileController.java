package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.profile.request.SessionBasicProfileUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.SessionBasicProfileResponse;
import com.umc.bscene.domain.session.enums.code.success.SessionSuccessCode;
import com.umc.bscene.domain.session.service.SessionBasicProfileService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sessions/profile")
public class SessionBasicProfileController {

    private final SessionBasicProfileService sessionBasicProfileService;

    @GetMapping
    public SuccessResponse<SessionBasicProfileResponse> getProfile(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        SessionBasicProfileResponse response = sessionBasicProfileService
                .getProfile(authMember.getUser().getId());
        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_BASIC_PROFILE_GET_SUCCESS
        );
    }

    @PatchMapping
    public SuccessResponse<SessionBasicProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody SessionBasicProfileUpdateRequest request
    ) {
        SessionBasicProfileResponse response = sessionBasicProfileService.updateProfile(
                authMember.getUser().getId(),
                request
        );
        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_BASIC_PROFILE_UPDATE_SUCCESS
        );
    }
}
