package com.umc.bscene.domain.user.controller;

import com.umc.bscene.domain.user.dto.response.mypage.FanMyPageResponse;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;
import com.umc.bscene.domain.user.dto.response.InterestedPerformanceResponse;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse;
import com.umc.bscene.domain.user.enums.HistoryYearFilter;
import com.umc.bscene.domain.user.response.code.UserSuccessCode;
import com.umc.bscene.domain.user.service.MyPageService;
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
public class MyPageController {

    private final MyPageService myPageService;

    // 마이페이지 조회 API (팬/밴드 모드 공용 엔드포인트)
    @GetMapping("/users/me")
    public ResponseEntity<SuccessResponse<FanMyPageResponse>> getFanMyPage(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        FanMyPageResponse response = myPageService.getFanMyPage(authMember.getUser());
        SuccessResponse<FanMyPageResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.FAN_MYPAGE_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 공연 참여 기록 조회 API (참여 완료 공연, 연도 필터, offset 무한스크롤)
    @GetMapping("/users/me/performance/history")
    public ResponseEntity<SuccessResponse<ParticipationHistoryResponse>> getPerformanceHistory(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "ALL") HistoryYearFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ParticipationHistoryResponse response = myPageService.getParticipationHistory(
                authMember.getUser().getId(), filter, page, size);
        SuccessResponse<ParticipationHistoryResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.FAN_PERFORMANCE_HISTORY_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 관심 공연 목록 조회 API (알림/참여 상태 포함, offset 무한스크롤)
    @GetMapping("/users/me/performance/interest")
    public ResponseEntity<SuccessResponse<InterestedPerformanceResponse>> getInterestedPerformances(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        InterestedPerformanceResponse response = myPageService.getInterestedPerformances(
                authMember.getUser().getId(), page, size);
        SuccessResponse<InterestedPerformanceResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.FAN_PERFORMANCE_INTEREST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 팔로우한 밴드 목록 조회 API (밴드명 가나다순, offset 무한스크롤)
    @GetMapping("/users/me/follows")
    public ResponseEntity<SuccessResponse<FollowedBandResponse>> getFollowedBands(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        FollowedBandResponse response = myPageService.getFollowedBands(
                authMember.getUser().getId(), page, size);
        SuccessResponse<FollowedBandResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.FAN_FOLLOWED_BANDS_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
