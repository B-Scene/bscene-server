package com.umc.bscene.domain.user.controller;

import com.umc.bscene.domain.user.dto.request.MyInfoUpdateRequest;
import com.umc.bscene.domain.user.dto.request.SessionApplyConfirmRequest;
import com.umc.bscene.domain.user.dto.request.SessionRecruitDecisionRequest;
import com.umc.bscene.domain.user.dto.request.UserModeUpdateRequest;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;
import com.umc.bscene.domain.user.dto.response.InterestedPerformanceResponse;
import com.umc.bscene.domain.user.dto.response.MyInfoResponse;
import com.umc.bscene.domain.user.dto.response.MyProfileResponse;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse;
import com.umc.bscene.domain.user.dto.response.mypage.BandMyPageResponse;
import com.umc.bscene.domain.user.dto.response.mypage.FanMyPageResponse;
import com.umc.bscene.domain.user.dto.response.mypage.MyPageResponse;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.enums.HistoryYearFilter;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import com.umc.bscene.domain.user.response.code.UserSuccessCode;
import com.umc.bscene.domain.user.service.UserService;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 마이페이지 조회 API (팬/밴드 모드 공용 엔드포인트)
    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        UserMode currentMode = authMember.getUser().getCurrentMode();

        ResponseEntity<SuccessResponse<MyPageResponse>> response;

        if (currentMode.equals(UserMode.FAN)) {
            FanMyPageResponse resDto = userService.getFanMyPage(authMember.getUser());
            SuccessResponse<MyPageResponse> successResponse = SuccessResponse.of(
                    resDto,
                    UserSuccessCode.FAN_MYPAGE_GET_SUCCESS
            );
            response = ResponseEntity.status(successResponse.getStatus()).body(successResponse);
        } else {
            BandMyPageResponse resDto = userService.getBandMyPage(authMember.getUser());
            response = ResponseEntity
                    .status(HttpStatus.OK)
                    .body(SuccessResponse.ok(resDto));
        }

        return response;
    }

    // 내가 속한 밴드 프로필과 나의 팬모드 프로필을 목록 조회.
    @GetMapping("/me/profiles")
    public ResponseEntity<SuccessResponse<?>> getMyProfiles(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "band") String type
    ) {
        if (!type.equals("band") && !type.equals("all")) {
            throw new UserException(UserErrorCode.PARAM_BAD_REQUEST);
        }

        MyProfileResponse response = userService.findMyProfiles(authMember.getUser(), type);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.ok(response));
    }

    @PatchMapping("/me/mode")
    public ResponseEntity<SuccessResponse<?>> updateUserMode(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody UserModeUpdateRequest request
    ) {

        userService.updateMode(authMember.getUser(), request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.empty(null));
    }

    // 공연 참여 기록 조회 API (참여 완료 공연, 연도 필터, offset 무한스크롤)
    @GetMapping("/me/performance/history")
    public ResponseEntity<SuccessResponse<ParticipationHistoryResponse>> getPerformanceHistory(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "ALL") HistoryYearFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ParticipationHistoryResponse response = userService.getParticipationHistory(
                authMember.getUser().getId(), filter, page, size);
        SuccessResponse<ParticipationHistoryResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.FAN_PERFORMANCE_HISTORY_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 관심 공연 목록 조회 API (알림/참여 상태 포함, offset 무한스크롤)
    @GetMapping("/me/performance/interest")
    public ResponseEntity<SuccessResponse<InterestedPerformanceResponse>> getInterestedPerformances(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "ALL") HistoryYearFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        InterestedPerformanceResponse response = userService.getInterestedPerformances(
                authMember.getUser().getId(), filter, page, size);
        SuccessResponse<InterestedPerformanceResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.FAN_PERFORMANCE_INTEREST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 내 정보 조회 API (내 정보 수정 화면 초기값 : 닉네임/관심 장르/활동 지역)
    @GetMapping("/me/information")
    public ResponseEntity<SuccessResponse<MyInfoResponse>> getMyInfo(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        MyInfoResponse response = userService.getMyInfo(authMember.getUser());
        SuccessResponse<MyInfoResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.MY_INFO_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 내 정보 수정 API (닉네임/관심 장르/활동 지역을 통째로 교체)
    @PatchMapping("/me/information")
    public ResponseEntity<SuccessResponse<MyInfoResponse>> updateMyInfo(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MyInfoUpdateRequest request
    ) {
        MyInfoResponse response = userService.updateMyInfo(authMember.getUser().getId(), request);
        SuccessResponse<MyInfoResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.MY_INFO_UPDATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 팔로우한 밴드 목록 조회 API (밴드명 가나다순, offset 무한스크롤)
    @GetMapping("/me/follows")
    public ResponseEntity<SuccessResponse<FollowedBandResponse>> getFollowedBands(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        FollowedBandResponse response = userService.getFollowedBands(
                authMember.getUser().getId(), page, size);
        SuccessResponse<FollowedBandResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.FAN_FOLLOWED_BANDS_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 나의 현재 활성화된 밴드 프로필을 사용하여, 내 밴드에 지원한 지원자들을 공고마다 섹셔닝
    // status=OPEN(진행 중, 마감 임박순) / CLOSE(마감, 최근 마감 순), 공고 단위 커서 페이지네이션
    @GetMapping("/me/recruitments/receives")
    public ResponseEntity<SuccessResponse<CursorPage<SessionRecruitmentResponse>>> getReceiveRecruitments(
            @AuthenticationPrincipal AuthMember user,
            @RequestParam(defaultValue = "OPEN") RecruitmentStatusFilter status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {

        CursorPage<SessionRecruitmentResponse> response =
                userService.findMyBandsRecruitments(user.getUser(), status, cursor, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.ok(response));
    }

    @PostMapping("/me/{applySubmissionId}/acceptance")
    public ResponseEntity<SuccessResponse<?>> decideSessionApply(
            @AuthenticationPrincipal AuthMember member,
            @PathVariable Long applySubmissionId,
            @Valid @RequestBody SessionRecruitDecisionRequest request
    ) {

        userService.decideSessionApply(member.getUser().getId(), applySubmissionId, request.isApproved());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.empty(null));
    }

    // 밴드가 수락한(BAND_ACCEPTED) 세션 지원 건에 대한 지원자의 최종 수락/거절
    // 최종 수락 시 활동명·파트를 입력받아 확정하고, 세션 멤버 등록까지 한 트랜잭션으로 수행
    @PostMapping("/me/{applySubmissionId}/final")
    public ResponseEntity<SuccessResponse<?>> confirmSessionApply(
            @AuthenticationPrincipal AuthMember member,
            @PathVariable Long applySubmissionId,
            @Valid @RequestBody SessionApplyConfirmRequest request
    ) {

        userService.confirmSessionApply(member.getUser().getId(), applySubmissionId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.empty(null));
    }
}
