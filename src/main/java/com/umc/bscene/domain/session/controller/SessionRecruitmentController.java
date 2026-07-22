package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentCreateRequest;
import com.umc.bscene.domain.session.dto.application.request.SessionApplicationSubmitRequest;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSubmitResponse;
import com.umc.bscene.domain.session.dto.application.response.SubmittedApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.InterestedRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.RecentRecruitmentListResponse;
import com.umc.bscene.domain.session.service.SessionRecruitmentInterestService;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentUpdateRequest;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;
import com.umc.bscene.domain.session.enums.code.success.SessionSuccessCode;
import com.umc.bscene.domain.session.service.SessionRecruitmentCommandService;
import com.umc.bscene.domain.session.service.SessionApplicationCommandService;
import com.umc.bscene.domain.session.service.SessionApplicationQueryService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.converter.SessionGenreJsonDeserializer;
import com.umc.bscene.domain.session.converter.SessionRegionJsonDeserializer;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.SessionRecruitmentSortType;
import com.umc.bscene.domain.session.service.SessionRecruitmentQueryService;
import com.umc.bscene.domain.session.service.SessionRecruitmentSearchKeywordService;
import com.umc.bscene.domain.session.dto.recruitment.response.RecruitmentSearchKeywordResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import java.util.List;
@RestController
@RequiredArgsConstructor
@RequestMapping("/sessions/recruitments")
public class SessionRecruitmentController {

    private final SessionRecruitmentCommandService sessionRecruitmentCommandService;
    private final SessionApplicationCommandService sessionApplicationCommandService;
    private final SessionApplicationQueryService sessionApplicationQueryService;
    private final SessionRecruitmentInterestService sessionRecruitmentInterestService;
    private final SessionRecruitmentQueryService sessionRecruitmentQueryService;
    private final SessionRecruitmentSearchKeywordService searchKeywordService;

    @GetMapping("/search-history")
    public SuccessResponse<List<RecruitmentSearchKeywordResponse>> getSearchHistory(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        return SuccessResponse.of(
                searchKeywordService.getAll(authMember.getUser().getId()),
                SessionSuccessCode.SESSION_RECRUITMENT_SEARCH_HISTORY_SUCCESS
        );
    }

    @DeleteMapping("/search-history/{keywordId}")
    public SuccessResponse<Void> deleteSearchHistory(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long keywordId
    ) {
        searchKeywordService.delete(authMember.getUser().getId(), keywordId);
        return new SuccessResponse<>(
                null,
                SessionSuccessCode.SESSION_RECRUITMENT_SEARCH_HISTORY_DELETE_SUCCESS
        );
    }
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

    @PostMapping("/{sessionRecruitmentId}/applications")
    public SuccessResponse<SessionApplicationSubmitResponse> submitApplication(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionRecruitmentId,
            @Valid @RequestBody SessionApplicationSubmitRequest request
    ) {
        SessionApplicationSubmitResponse response = sessionApplicationCommandService
                .submitApplication(
                        authMember.getUser().getId(),
                        sessionRecruitmentId,
                        request.sessionApplicationId()
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_APPLICATION_SUBMIT_SUCCESS
        );
    }

    @GetMapping("/submissions/{applicationSubmissionId}")
    public SuccessResponse<SubmittedApplicationDetailResponse> getSubmittedApplication(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long applicationSubmissionId
    ) {
        SubmittedApplicationDetailResponse response = sessionApplicationQueryService
                .getSubmittedApplication(
                        authMember.getUser().getId(),
                        applicationSubmissionId
                );
        return SuccessResponse.of(
                response,
                SessionSuccessCode.SUBMITTED_APPLICATION_DETAIL_SUCCESS
        );
    }

    @GetMapping("/interests")
    public SuccessResponse<InterestedRecruitmentListResponse> getInterestedRecruitments(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        InterestedRecruitmentListResponse response = sessionRecruitmentInterestService
                .getMyInterests(authMember.getUser().getId(), cursorId, size);
        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_RECRUITMENT_INTEREST_LIST_SUCCESS
        );
    }

    @GetMapping("/recently-viewed")
    public SuccessResponse<RecentRecruitmentListResponse> getRecentRecruitments(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        RecentRecruitmentListResponse response = sessionRecruitmentQueryService
                .getRecentRecruitments(
                        authMember.getUser().getId(), cursorId, size
                );
        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_RECRUITMENT_RECENT_LIST_SUCCESS
        );
    }
    // 세션 모집 공고 목록 조회
    @GetMapping
    public SuccessResponse<SessionRecruitmentListResponse> getSessionRecruitments(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Part part,
            @RequestParam(required = false) SkillLevel skillLevel,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") SessionRecruitmentSortType sort,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        SessionRecruitmentListResponse response =
                sessionRecruitmentQueryService.getSessionRecruitments(
                        authMember.getUser().getId(),
                        part,
                        skillLevel,
                        SessionGenreJsonDeserializer.fromKorean(genre),
                        SessionRegionJsonDeserializer.fromKorean(region),
                        keyword,
                        sort,
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
    @GetMapping("/{sessionRecruitmentId}")
    public SuccessResponse<SessionRecruitmentDetailResponse> getSessionRecruitmentDetail(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionRecruitmentId
    ) {

        SessionRecruitmentDetailResponse response =
                sessionRecruitmentQueryService.getSessionRecruitmentDetail(
                        authMember.getUser().getId(),
                        sessionRecruitmentId
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_RECRUITMENT_DETAIL_SUCCESS
        );
    }

}
