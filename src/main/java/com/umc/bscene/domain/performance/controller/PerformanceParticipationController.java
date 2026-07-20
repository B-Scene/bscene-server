package com.umc.bscene.domain.performance.controller;

import com.umc.bscene.domain.performance.dto.response.PendingParticipationResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceParticipationDeclineResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceParticipationResponse;
import com.umc.bscene.domain.performance.response.code.PerformanceSuccessCode;
import com.umc.bscene.domain.performance.service.PerformanceAlarmService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/performances")
public class PerformanceParticipationController {

    private final PerformanceAlarmService performanceAlarmService;

    // 참여 확인 대기 공연 목록 조회 API (팬모드 홈 '공연은 어떠셨나요?' 모달용)
    @GetMapping("/participation/pending")
    public ResponseEntity<SuccessResponse<PendingParticipationResponse>> getPendingParticipations(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        PendingParticipationResponse response = performanceAlarmService.getPendingParticipations(
                authMember.getUser().getId());
        SuccessResponse<PendingParticipationResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_PENDING_PARTICIPATION_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 참여 완료 API (참여 예정 → 참여 완료)
    @PatchMapping("/{performanceId}/participation/complete")
    public ResponseEntity<SuccessResponse<PerformanceParticipationResponse>> completeParticipation(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId
    ) {
        PerformanceParticipationResponse response = performanceAlarmService.completeParticipation(
                authMember.getUser().getId(), performanceId);
        SuccessResponse<PerformanceParticipationResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_PARTICIPATION_COMPLETE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 불참 처리 API (참여 예정 기록 삭제, 관심 공연은 유지)
    @DeleteMapping("/{performanceId}/participation")
    public ResponseEntity<SuccessResponse<PerformanceParticipationDeclineResponse>> declineParticipation(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId
    ) {
        PerformanceParticipationDeclineResponse response = performanceAlarmService.declineParticipation(
                authMember.getUser().getId(), performanceId);
        SuccessResponse<PerformanceParticipationDeclineResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_PARTICIPATION_DECLINE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
