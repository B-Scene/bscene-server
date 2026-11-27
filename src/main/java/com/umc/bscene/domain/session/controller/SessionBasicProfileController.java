package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.controller.docs.SessionBasicProfileApi;
import com.umc.bscene.domain.session.dto.profile.request.SessionNameUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.request.SessionProfileImageUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.SessionBasicProfileResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionNameUpdateResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionProfileImageUpdateResponse;
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
public class SessionBasicProfileController implements SessionBasicProfileApi {

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

    @PatchMapping("/name")
    public SuccessResponse<SessionNameUpdateResponse> updateName(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody SessionNameUpdateRequest request
    ) {
        SessionNameUpdateResponse response = sessionBasicProfileService
                .updateName(authMember.getUser().getId(), request.name());
        return SuccessResponse.of(response, SessionSuccessCode.SESSION_NAME_UPDATE_SUCCESS);
    }

    @PatchMapping("/image")
    public SuccessResponse<SessionProfileImageUpdateResponse> updateProfileImage(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody SessionProfileImageUpdateRequest request
    ) {
        SessionProfileImageUpdateResponse response = sessionBasicProfileService
                .updateProfileImage(authMember.getUser().getId(), request.profileImageUrl());
        return SuccessResponse.of(response, SessionSuccessCode.SESSION_PROFILE_IMAGE_UPDATE_SUCCESS);
    }
}
