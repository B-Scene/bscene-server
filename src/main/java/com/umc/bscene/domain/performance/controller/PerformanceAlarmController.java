package com.umc.bscene.domain.performance.controller;

import com.umc.bscene.domain.performance.dto.response.PerformanceAlarmResponse;
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
@RequestMapping("/performances/{performanceId}/alarm")
public class PerformanceAlarmController {

    private final PerformanceAlarmService performanceAlarmService;

    // 공연 알림 설정 API
    @PostMapping
    public ResponseEntity<SuccessResponse<PerformanceAlarmResponse>> setAlarm(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId
    ) {
        PerformanceAlarmResponse response = performanceAlarmService.setAlarm(authMember.getUser().getId(), performanceId);
        SuccessResponse<PerformanceAlarmResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_ALARM_SET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 알림 해제 API
    @DeleteMapping
    public ResponseEntity<SuccessResponse<PerformanceAlarmResponse>> unsetAlarm(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId
    ) {
        PerformanceAlarmResponse response = performanceAlarmService.unsetAlarm(authMember.getUser().getId(), performanceId);
        SuccessResponse<PerformanceAlarmResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_ALARM_UNSET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
