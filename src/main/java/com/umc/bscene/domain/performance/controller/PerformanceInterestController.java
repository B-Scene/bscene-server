package com.umc.bscene.domain.performance.controller;

import com.umc.bscene.domain.performance.dto.response.PerformanceInterestResponse;
import com.umc.bscene.domain.performance.response.code.PerformanceSuccessCode;
import com.umc.bscene.domain.performance.service.PerformanceInterestService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/performances/{performanceId}/interest")
public class PerformanceInterestController {

    private final PerformanceInterestService performanceInterestService;

    // 관심 공연 등록 API
    @PostMapping
    public ResponseEntity<SuccessResponse<PerformanceInterestResponse>> setInterest(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId
    ) {
        PerformanceInterestResponse response = performanceInterestService.setInterest(authMember.getUser().getId(), performanceId);
        SuccessResponse<PerformanceInterestResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_INTEREST_SET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 관심 공연 해제 API
    @DeleteMapping
    public ResponseEntity<SuccessResponse<PerformanceInterestResponse>> unsetInterest(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId
    ) {
        PerformanceInterestResponse response = performanceInterestService.unsetInterest(authMember.getUser().getId(), performanceId);
        SuccessResponse<PerformanceInterestResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_INTEREST_UNSET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
