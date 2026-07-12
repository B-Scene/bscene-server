package com.umc.bscene.domain.fanhome.controller;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse;
import com.umc.bscene.domain.fanhome.dto.response.FollowingBandNewsResponse;
import com.umc.bscene.domain.fanhome.dto.response.UpcomingPerformanceResponse;
import com.umc.bscene.domain.fanhome.enums.UpcomingSortType;
import com.umc.bscene.domain.fanhome.response.code.FanHomeSuccessCode;
import com.umc.bscene.domain.fanhome.service.FanHomeService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FanHomeController {

    private final FanHomeService fanHomeService;

    // 팬모드 홈 통합 조회 API
    @GetMapping("/home")
    public ResponseEntity<SuccessResponse<FanHomeResponse>> getFanHome(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        FanHomeResponse response = fanHomeService.getFanHome(authMember.getUser().getId());
        SuccessResponse<FanHomeResponse> successResponse = SuccessResponse.of(
                response,
                FanHomeSuccessCode.FAN_HOME_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 팔로우한 밴드 소식 전체 조회 API (커서 기반 무한스크롤)
    @GetMapping("/posts/following")
    public ResponseEntity<SuccessResponse<FollowingBandNewsResponse>> getFollowingBandNews(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        FollowingBandNewsResponse response = fanHomeService.getFollowingBandNews(
                authMember.getUser().getId(), cursor, size);
        SuccessResponse<FollowingBandNewsResponse> successResponse = SuccessResponse.of(
                response,
                FanHomeSuccessCode.FOLLOWING_BAND_NEWS_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 다가오는 공연 전체 목록 조회 API (offset 기반 무한스크롤, 임박/최신/인기순)
    @GetMapping("/performances/upcoming")
    public ResponseEntity<SuccessResponse<UpcomingPerformanceResponse>> getUpcomingPerformances(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "IMMINENT") UpcomingSortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UpcomingPerformanceResponse response = fanHomeService.getUpcomingPerformances(
                authMember.getUser().getId(), sort, page, size);
        SuccessResponse<UpcomingPerformanceResponse> successResponse = SuccessResponse.of(
                response,
                FanHomeSuccessCode.UPCOMING_PERFORMANCE_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
