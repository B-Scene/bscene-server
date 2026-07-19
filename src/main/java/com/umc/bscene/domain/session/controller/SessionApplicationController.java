package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationCreateRequest;
import com.umc.bscene.domain.session.dto.application.request.SessionApplicationVisibilityRequest;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSearchResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationSummaryResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationVisibilityResponse;
import com.umc.bscene.domain.session.dto.application.response.MyApplicationSubmissionListResponse;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.code.SessionSuccessCode;
import com.umc.bscene.domain.session.service.SessionApplicationCommandService;
import com.umc.bscene.domain.session.service.SessionApplicationQueryService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sessions/applications")
public class SessionApplicationController {

    private final SessionApplicationQueryService sessionApplicationQueryService;
    private final SessionApplicationCommandService sessionApplicationCommandService;

    @GetMapping("/summary")
    public SuccessResponse<MySessionApplicationSummaryResponse> getMySessionApplicationSummary(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        MySessionApplicationSummaryResponse response = sessionApplicationQueryService
                .getMySessionApplicationSummary(authMember.getUser().getId());

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_SESSION_APPLICATION_SUMMARY_SUCCESS
        );
    }

    @GetMapping("/submissions")
    public SuccessResponse<MyApplicationSubmissionListResponse> getMyApplicationSubmissions(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        MyApplicationSubmissionListResponse response = sessionApplicationQueryService
                .getMyApplicationSubmissions(
                        authMember.getUser().getId(),
                        cursorId,
                        size
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_APPLICATION_SUBMISSION_LIST_SUCCESS
        );
    }

    @GetMapping("/submissions/{applicationSubmissionId}/application")
    public SuccessResponse<SessionApplicationDetailResponse> getMySubmittedApplication(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long applicationSubmissionId
    ) {
        SessionApplicationDetailResponse response = sessionApplicationQueryService
                .getMySubmittedApplication(
                        authMember.getUser().getId(),
                        applicationSubmissionId
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_SUBMITTED_APPLICATION_DETAIL_SUCCESS
        );
    }

    @DeleteMapping("/submissions/{applicationSubmissionId}")
    public SuccessResponse<Void> cancelApplicationSubmission(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long applicationSubmissionId
    ) {
        sessionApplicationCommandService.cancelSubmission(
                authMember.getUser().getId(),
                applicationSubmissionId
        );

        return new SuccessResponse<>(
                null,
                SessionSuccessCode.APPLICATION_SUBMISSION_CANCEL_SUCCESS
        );
    }

    @GetMapping("/{sessionApplicationId}")
    public SuccessResponse<SessionApplicationDetailResponse> getSessionApplicationDetail(
            @PathVariable Long sessionApplicationId
    ) {
        SessionApplicationDetailResponse response = sessionApplicationQueryService
                .getDefaultApplicationDetail(sessionApplicationId);

        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_APPLICATION_DETAIL_SUCCESS
        );
    }

    @GetMapping("/search")
    public SuccessResponse<SessionApplicationSearchResponse> searchSessionApplications(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) SessionRegion region,
            @RequestParam(required = false) SkillLevel skillLevel,
            @RequestParam(required = false) Part part,
            @RequestParam(required = false) SessionGenre genre,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        SessionApplicationSearchResponse response = sessionApplicationQueryService
                .searchDefaultApplications(
                        authMember.getUser().getId(),
                        region,
                        skillLevel,
                        part,
                        genre,
                        keyword,
                        cursorId,
                        size
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.SESSION_APPLICATION_SEARCH_SUCCESS
        );
    }

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
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<MySessionApplicationResponse> createSessionApplication(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MySessionApplicationCreateRequest request
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

    @DeleteMapping("/{sessionApplicationId}")
    public SuccessResponse<Void> deleteSessionApplication(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionApplicationId
    ) {
        sessionApplicationCommandService.deleteSessionApplication(
                authMember.getUser().getId(),
                sessionApplicationId
        );

        return new SuccessResponse<>(
                null,
                SessionSuccessCode.MY_SESSION_APPLICATION_DELETE_SUCCESS
        );
    }

    @PatchMapping("/{sessionApplicationId}/visibility")
    public SuccessResponse<SessionApplicationVisibilityResponse> updateVisibility(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long sessionApplicationId,
            @Valid @RequestBody SessionApplicationVisibilityRequest request
    ) {
        SessionApplicationVisibilityResponse response = sessionApplicationCommandService
                .updateVisibility(
                        authMember.getUser().getId(),
                        sessionApplicationId,
                        request
                );

        return SuccessResponse.of(
                response,
                SessionSuccessCode.MY_SESSION_APPLICATION_VISIBILITY_UPDATE_SUCCESS
        );
    }
}
