package com.umc.bscene.domain.fanhome.controller;

import com.umc.bscene.domain.fanhome.dto.response.DatePerformanceResponse;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse;
import com.umc.bscene.domain.fanhome.dto.response.FollowingBandNewsResponse;
import com.umc.bscene.domain.fanhome.dto.response.PerformanceCalendarResponse;
import com.umc.bscene.domain.fanhome.dto.response.RecommendedPerformanceResponse;
import com.umc.bscene.domain.fanhome.dto.response.UpcomingPerformanceResponse;
import com.umc.bscene.domain.fanhome.enums.RecommendSortType;
import com.umc.bscene.domain.fanhome.enums.UpcomingSortType;
import com.umc.bscene.domain.fanhome.response.code.FanHomeSuccessCode;
import com.umc.bscene.domain.fanhome.service.FanHomeService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    // 추천 공연 전체 목록 조회 API (offset 기반 무한스크롤, 인기/임박순)
    // 팔로우 밴드의 다가오는 공연이 없어 홈 공연 섹션이 RECOMMENDED일 때의 더보기 화면
    @GetMapping("/performances/recommend")
    public ResponseEntity<SuccessResponse<RecommendedPerformanceResponse>> getRecommendedPerformances(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "POPULAR") RecommendSortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        RecommendedPerformanceResponse response = fanHomeService.getRecommendedPerformances(
                authMember.getUser().getId(), sort, page, size);
        SuccessResponse<RecommendedPerformanceResponse> successResponse = SuccessResponse.of(
                response,
                FanHomeSuccessCode.RECOMMENDED_PERFORMANCE_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 달력 조회 API (year/month 없으면 서버 기준 이번 달)
    @GetMapping("/performances/calendar")
    public ResponseEntity<SuccessResponse<PerformanceCalendarResponse>> getPerformanceCalendar(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        PerformanceCalendarResponse response = fanHomeService.getPerformanceCalendar(
                authMember.getUser().getId(), year, month);
        SuccessResponse<PerformanceCalendarResponse> successResponse = SuccessResponse.of(
                response,
                FanHomeSuccessCode.PERFORMANCE_CALENDAR_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 특정 날짜 공연 목록 조회 API (date 없으면 서버 기준 오늘, offset 기반 무한스크롤, 시간 빠른 순)
    @GetMapping("/performances/by-date")
    public ResponseEntity<SuccessResponse<DatePerformanceResponse>> getPerformancesByDate(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        DatePerformanceResponse response = fanHomeService.getPerformancesByDate(
                authMember.getUser().getId(), date, page, size);
        SuccessResponse<DatePerformanceResponse> successResponse = SuccessResponse.of(
                response,
                FanHomeSuccessCode.DATE_PERFORMANCE_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
