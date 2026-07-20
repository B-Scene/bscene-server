package com.umc.bscene.domain.performance.controller;

import com.umc.bscene.domain.performance.dto.request.PerformanceCreateRequest;
import com.umc.bscene.domain.performance.dto.request.PerformanceUpdateRequest;
import com.umc.bscene.domain.performance.dto.response.PerformanceListResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceResponse;
import com.umc.bscene.domain.performance.response.code.PerformanceSuccessCode;
import com.umc.bscene.domain.performance.service.PerformanceService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    // 공연 등록 API (밴드 멤버만 가능)
    @PostMapping("/bands/{bandId}/performances")
    public ResponseEntity<SuccessResponse<PerformanceResponse>> createPerformance(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId,
            @Valid @RequestBody PerformanceCreateRequest request
    ) {
        PerformanceResponse response = performanceService.createPerformance(authMember.getUser().getId(), bandId, request);
        SuccessResponse<PerformanceResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_CREATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 목록 조회 API (일정 탭)
    @GetMapping("/bands/{bandId}/performances")
    public ResponseEntity<SuccessResponse<PerformanceListResponse>> getPerformances(
            @PathVariable Long bandId
    ) {
        PerformanceListResponse response = performanceService.getPerformances(bandId);
        SuccessResponse<PerformanceListResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_LIST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 상세 조회 API
    @GetMapping("/performances/{performanceId}")
    public ResponseEntity<SuccessResponse<PerformanceResponse>> getPerformanceDetail(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId
    ) {
        PerformanceResponse response = performanceService.getPerformanceDetail(authMember.getUser().getId(), performanceId);
        SuccessResponse<PerformanceResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_DETAIL_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 정보 수정 API (등록한 밴드의 멤버만 가능)
    @PatchMapping("/performances/{performanceId}")
    public ResponseEntity<SuccessResponse<PerformanceResponse>> updatePerformance(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId,
            @RequestBody PerformanceUpdateRequest request
    ) {
        PerformanceResponse response = performanceService.updatePerformance(authMember.getUser().getId(), performanceId, request);
        SuccessResponse<PerformanceResponse> successResponse = SuccessResponse.of(
                response,
                PerformanceSuccessCode.PERFORMANCE_UPDATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 삭제 API (등록한 밴드의 멤버만 가능)
    @DeleteMapping("/performances/{performanceId}")
    public ResponseEntity<SuccessResponse<Void>> deletePerformance(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long performanceId
    ) {
        performanceService.deletePerformance(authMember.getUser().getId(), performanceId);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                PerformanceSuccessCode.PERFORMANCE_DELETE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
