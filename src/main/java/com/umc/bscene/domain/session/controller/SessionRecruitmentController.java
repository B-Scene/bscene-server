package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.application.request.SessionApplicationStatusRequest;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationStatusResponse;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentCreateRequest;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentUpdateRequest;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.code.SessionSuccessCode;
import com.umc.bscene.domain.session.service.SessionApplicationService;
import com.umc.bscene.domain.session.service.SessionRecruitmentCommandService;
import com.umc.bscene.domain.session.service.SessionRecruitmentQueryService;
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

    private final SessionApplicationService sessionApplicationService;
    private final SessionRecruitmentCommandService sessionRecruitmentCommandService;
    private final SessionRecruitmentQueryService sessionRecruitmentQueryService;

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
                SessionSuccessCode.SESSION_RECRUITMENT_CREATE_SUCCESS
        );
    }

    // 세션 모집 공고 목록 조회
    @GetMapping
    public SuccessResponse<SessionRecruitmentListResponse> getSessionRecruitments(
            @RequestParam(required = false) Part part,
            @RequestParam(required = false) SessionGenre genre,
            @RequestParam(required = false) SessionRegion region,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        SessionRecruitmentListResponse response =
                sessionRecruitmentQueryService.getSessionRecruitments(
                        part,
                        genre,
                        region,
                        keyword,
                        cursorId,
                        size
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_RECRUITMENT_LIST_SUCCESS
        );
    }

    @PatchMapping("/{sessionRecruitmentId}")
    public SuccessResponse<SessionRecruitmentCreateResponse> updateSessionRecruitment(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionRecruitmentId,
            @Valid @RequestBody SessionRecruitmentUpdateRequest request
    ) {
        Long userId = authMember.getUser().getId();

        SessionRecruitmentCreateResponse response =
                sessionRecruitmentCommandService.updateSessionRecruitment(
                        userId,
                        sessionRecruitmentId,
                        request
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_RECRUITMENT_UPDATE_SUCCESS
        );
    }

    @DeleteMapping("/{sessionRecruitmentId}")
    public SuccessResponse<Void> deleteSessionRecruitment(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionRecruitmentId
    ) {
        Long userId = authMember.getUser().getId();

        sessionRecruitmentCommandService.deleteSessionRecruitment(
                userId,
                sessionRecruitmentId
        );

        return new SuccessResponse<>(
                null,
                SessionSuccessCode.SESSION_RECRUITMENT_DELETE_SUCCESS
        );
    }

    // 세션 모집 공고 상세 조회
    @GetMapping("/{recruitmentId}")
    public SuccessResponse<SessionRecruitmentDetailResponse> getSessionRecruitmentDetail(
            @PathVariable Long recruitmentId
    ) {
        SessionRecruitmentDetailResponse response =
                sessionRecruitmentQueryService.getSessionRecruitmentDetail(recruitmentId);

        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_RECRUITMENT_DETAIL_SUCCESS
        );
    }

    // 세션 지원 승인 / 거절
    @PatchMapping("/{applicationId}/status")
    public SuccessResponse<SessionApplicationStatusResponse> updateApplicationStatus(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long applicationId,
            @Valid @RequestBody SessionApplicationStatusRequest request
    ) {
        Long bandId = sessionApplicationService.updateStatus(
                applicationId,
                request.isApproved()
        );

        SessionApplicationStatusResponse response =
                SessionApplicationStatusResponse.of(bandId);

        if (request.isApproved()) {
            return SuccessResponse.of(
                    response,
                    SessionSuccessCode.SESSION_APPLICATION_ACCEPT_SUCCESS
            );
        }

        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_APPLICATION_REJECT_SUCCESS
        );
    }
}