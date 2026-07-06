package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.profile.request.MySessionProfileUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.MySessionProfileResponse;
import com.umc.bscene.domain.session.enums.code.SessionProfileSuccessCode;
import com.umc.bscene.domain.session.service.SessionProfileCommandService;
import com.umc.bscene.domain.session.service.SessionProfileQueryService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class SessionProfileController {

    private final SessionProfileQueryService sessionProfileQueryService;
    private final SessionProfileCommandService sessionProfileCommandService;

    @GetMapping("/me/session-profile")
    public SuccessResponse<MySessionProfileResponse> getMySessionProfile(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long userId = authMember.getUser().getId();

        MySessionProfileResponse response =
                sessionProfileQueryService.getMySessionProfile(userId);

        if (!response.getHasProfile()) {
            return SuccessResponse.of(
                    response,
                    SessionProfileSuccessCode.MY_SESSION_PROFILE_EMPTY
            );
        }

        return SuccessResponse.of(
                response,
                SessionProfileSuccessCode.MY_SESSION_PROFILE_GET_SUCCESS
        );
    }

    @PutMapping("/me/session-profile")
    public SuccessResponse<MySessionProfileResponse> saveMySessionProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MySessionProfileUpdateRequest request
    ) {
        Long userId = authMember.getUser().getId();

        MySessionProfileResponse response =
                sessionProfileCommandService.saveMySessionProfile(userId, request);

        return SuccessResponse.of(
                response,
                SessionProfileSuccessCode.MY_SESSION_PROFILE_UPDATE_SUCCESS
        );
    }
}