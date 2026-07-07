package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.profile.request.MyBandProfileUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.MyBandProfileResponse;
import com.umc.bscene.domain.session.enums.code.SessionSuccessCode;
import com.umc.bscene.domain.session.service.BandProfileCommandService;
import com.umc.bscene.domain.session.service.BandProfileQueryService;
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
public class BandProfileController {

    private final BandProfileQueryService sessionProfileQueryService;
    private final BandProfileCommandService sessionProfileCommandService;

    @GetMapping("/me/session-profile")
    public SuccessResponse<MyBandProfileResponse> getMySessionProfile(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long userId = authMember.getUser().getId();

        MyBandProfileResponse response =
                sessionProfileQueryService.getMySessionProfile(userId);

        if (!response.getHasProfile()) {
            return SuccessResponse.of(
                    response,
                    SessionSuccessCode.MY_SESSION_PROFILE_EMPTY
            );
        }

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_SESSION_PROFILE_GET_SUCCESS
        );
    }

    @PutMapping("/me/session-profile")
    public SuccessResponse<MyBandProfileResponse> saveMySessionProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MyBandProfileUpdateRequest request
    ) {
        Long userId = authMember.getUser().getId();

        MyBandProfileResponse response =
                sessionProfileCommandService.saveMySessionProfile(userId, request);

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_SESSION_PROFILE_UPDATE_SUCCESS
        );
    }
}