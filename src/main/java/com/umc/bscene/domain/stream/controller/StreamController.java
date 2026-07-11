package com.umc.bscene.domain.stream.controller;

import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.LiveAlarmToggleResponse;
import com.umc.bscene.domain.stream.dto.response.LiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.dto.response.UpcomingLiveResponse;
import com.umc.bscene.domain.stream.enums.code.success.StreamSuccessCode;
import com.umc.bscene.domain.stream.service.StreamService;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/lives")
@Validated
public class StreamController {

    private final StreamService streamService;

    @PostMapping("/{liveId}")
    public ResponseEntity<SuccessResponse<StreamRoomResponse>> enter(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        StreamRoomResponse response = streamService.enterRoom(
                authMember.getUser().getId(), liveId
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(response, StreamSuccessCode.LIVE_ROOM_ENTER_SUCCESS));
    }

    @PostMapping("/{liveId}/leave")
    public ResponseEntity<SuccessResponse<Void>> leave(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {

        streamService.leaveRoom(authMember.getUser().getId(), liveId);

        // 첫 인자가 null이기 때문에, overloading 해석이 더 구체적인 ApiResponse.of로 매핑되는 이유로 인해 new 키워드 생성으로 수정
        SuccessResponse<Void> body = new SuccessResponse<>(null, StreamSuccessCode.LIVE_ROOM_LEAVE_SUCCESS);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    // 라이브 홈 통합 조회 (currentMode에 따라 팬/밴드 모드 분기)
    @GetMapping("/home")
    public ResponseEntity<SuccessResponse<LiveHomeResponse>> getLiveHome(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        LiveHomeResponse response = streamService.getLiveHome(authMember.getUser());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(response, StreamSuccessCode.LIVE_HOME_SUCCESS));
    }

    // 현재 라이브 중인 전체 목록 (모든 유저 동일 응답 → 서비스 계층에서 @Cacheable 캐싱)
    @GetMapping("/live-now/all")
    public ResponseEntity<SuccessResponse<CursorPage<LiveStreamResponse>>> getInLiveStreams(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size
    ) {
        CursorPage<LiveStreamResponse> page = streamService.getLiveStreams(cursor, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(page, StreamSuccessCode.ALL_LIVE_SUCCESS));
    }

    // 팔로우한 밴드의 현재 라이브 중 목록 (팬 모드 전용, 유저별 데이터라 캐싱 X)
    @GetMapping("/live-now/following")
    public ResponseEntity<SuccessResponse<CursorPage<LiveStreamResponse>>> getFollowingLiveStreams(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size
    ) {
        CursorPage<LiveStreamResponse> page = streamService.getFollowingLiveStreams(
                authMember.getUser(), cursor, size
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(page, StreamSuccessCode.FOLLOWING_LIVE_SUCCESS));
    }

    // 예정된 라이브 목록 (나의 알림 설정 여부 포함 → 유저별 데이터라 캐싱 X)
    @GetMapping("/scheduled")
    public ResponseEntity<SuccessResponse<CursorPage<UpcomingLiveResponse>>> getUpcomingLives(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "false") boolean following,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size
    ) {
        CursorPage<UpcomingLiveResponse> page = streamService.getUpcomingLives(
                authMember.getUser(), following, cursor, size
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(page, StreamSuccessCode.SCHEDULED_LIVE_SUCCESS));
    }

    // 예정된 라이브 알림 설정 토글
    @PostMapping("/{liveId}/alarm")
    public ResponseEntity<SuccessResponse<LiveAlarmToggleResponse>> toggleLiveAlarm(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        LiveAlarmToggleResponse response = streamService.toggleLiveAlarm(authMember.getUser(), liveId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(response, StreamSuccessCode.LIVE_ALARM_TOGGLE_SUCCESS));
    }

    /*
     * TODO: [다시보기 조회 API - 구현 보류]
     * 다시보기 테이블이 아직 없음. 원본 브랜치(feat/#86-audio-streaming-additional-crud)에서
     * 라이브 종료 후 재개 가능한 업로드로 다시보기를 생성하는 로직이 완성되기 전까지
     * 아래 단계만 기록해두고 구현하지 않는다.
     *
     * 1. Replay 엔티티/테이블 생성: 밴드 테이블 PK(band_id)를 FK로 참조
     *    (replay_id PK, band_id FK, title, playbackUrl, viewCount, duration 등)
     * 2. GET /lives/replays?following={bool}&cursor={id}&size={1~15}&sort={LATEST|POPULAR}
     *    - 팔로우 탭(following=true): userId → FollowPort로 팔로우 밴드 ID 조회 → band_id in (...)
     *      커서 페이징(size 최소 1 / 기본 10 / 최대 15), 정렬 최신순(id desc) / 인기순(다시보기 조회수 내림차순)
     *    - 전체 탭(following=false): 밴드 필터 없이 동일 쿼리, @Cacheable(CacheConfig.REPLAY_ALL) 캐싱
     * 3. 팬모드 홈(/lives/home)의 다시보기 섹션: 최신 다시보기 8개 노출
     */

    @PostMapping
    public ResponseEntity<SuccessResponse<StreamCreateResponse>> createAudioStream(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody StreamCreateRequest request
    ) {
        StreamCreateResponse response = streamService.createStream(
                authMember.getUser(), request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.of(response, StreamSuccessCode.LIVE_CREATE_SUCCESS));
    }

    @PostMapping("/{liveId}/close")
    public ResponseEntity<SuccessResponse<Void>> closeAudioStream(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        streamService.closeStream(authMember.getUser().getId(), liveId);
        SuccessResponse<Void> body = new SuccessResponse<>(null, StreamSuccessCode.LIVE_CLOSE_SUCCESS);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @PostMapping("/{liveId}/reports")
    public ResponseEntity<SuccessResponse<?>> reportUser(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId,
            @RequestBody(required = false) Object request  // FIXME: 비즈니스 로직 완성 시 ReportUserRequest로 교체
    ) {

        // Business Logic

        // FIXME: 비즈니스 로직 완성 시 업데이트
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.empty(null));
    }

    @GetMapping(value = "/{liveId}/viewers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeViewer(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId,
            HttpServletResponse response
    ) {
        // 리버스 프록시(nginx 등)가 이 SSE 응답만 버퍼링하지 않도록(실시간 전송)
        response.setHeader("X-Accel-Buffering", "no");
        return streamService.subscribeViewerCount(authMember.getUser().getId(), liveId);
    }
}
