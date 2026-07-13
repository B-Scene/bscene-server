package com.umc.bscene.domain.performance.controller;

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
@RequestMapping("/performances/{performanceId}/participation")
public class PerformanceParticipationController {

    private final PerformanceAlarmService performanceAlarmService;

    // 공연 참여 완료 API (참여 예정 → 참여 완료)
    @PatchMapping("/complete")
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
}
