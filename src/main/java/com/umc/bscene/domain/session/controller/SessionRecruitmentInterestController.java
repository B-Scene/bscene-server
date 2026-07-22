package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentInterestResponse;
import com.umc.bscene.domain.session.enums.code.success.SessionSuccessCode;
import com.umc.bscene.domain.session.service.SessionRecruitmentInterestService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sessions/recruitments/{sessionRecruitmentId}/interest")
public class SessionRecruitmentInterestController {

    private final SessionRecruitmentInterestService interestService;

    @PostMapping
    public ResponseEntity<SuccessResponse<SessionRecruitmentInterestResponse>> setInterest(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionRecruitmentId
    ) {
        SessionRecruitmentInterestResponse response = interestService.setInterest(
                authMember.getUser().getId(),
                sessionRecruitmentId
        );
        SuccessResponse<SessionRecruitmentInterestResponse> successResponse = SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_RECRUITMENT_INTEREST_SET_SUCCESS
        );
        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    @DeleteMapping
    public ResponseEntity<SuccessResponse<SessionRecruitmentInterestResponse>> unsetInterest(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionRecruitmentId
    ) {
        SessionRecruitmentInterestResponse response = interestService.unsetInterest(
                authMember.getUser().getId(),
                sessionRecruitmentId
        );
        SuccessResponse<SessionRecruitmentInterestResponse> successResponse = SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_RECRUITMENT_INTEREST_UNSET_SUCCESS
        );
        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
