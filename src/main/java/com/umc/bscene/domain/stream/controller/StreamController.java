package com.umc.bscene.domain.stream.controller;

import com.umc.bscene.domain.stream.dto.request.ReportUserRequest;
import com.umc.bscene.domain.stream.dto.request.ReservationPatchRequest;
import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.LiveAlarmToggleResponse;
import com.umc.bscene.domain.stream.dto.response.LiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveMembersResponse;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.ReplayResponse;
import com.umc.bscene.domain.stream.dto.response.ReservationEditResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamReplayResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.dto.response.StreamSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.UpcomingLiveResponse;
import com.umc.bscene.domain.stream.enums.ReplaySort;
import com.umc.bscene.domain.stream.enums.code.success.StreamSuccessCode;
import com.umc.bscene.domain.stream.service.StreamReplayService;
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
    private final StreamReplayService streamReplayService;

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

    // 라이브 방 멤버(송출자 + 공동 진행자) 프로필 요약 목록 조회
    @GetMapping("/{liveId}/members")
    public ResponseEntity<SuccessResponse<LiveMembersResponse>> getLiveMembers(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        LiveMembersResponse response = streamService.getLiveMembers(liveId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(response, StreamSuccessCode.LIVE_MEMBERS_SUCCESS));
    }

    @GetMapping("/{liveId}/summary")
    public ResponseEntity<SuccessResponse<StreamSummaryResponse>> getStreamSummary(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        StreamSummaryResponse response = streamService.getStreamSummary(authMember.getUser().getId(), liveId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(response, StreamSuccessCode.LIVE_SUMMARY_SUCCESS));
    }

    @PostMapping("/{liveId}/replay")
    public ResponseEntity<SuccessResponse<Void>> requestReplayUpload(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        streamReplayService.requestReplayUpload(authMember.getUser().getId(), liveId);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new SuccessResponse<>(null, StreamSuccessCode.REPLAY_SAVE_REQUEST_SUCCESS));
    }

    @GetMapping("/{liveId}/replay")
    public ResponseEntity<SuccessResponse<StreamReplayResponse>> watchReplay(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        StreamReplayResponse response = streamReplayService.watchReplay(liveId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(response, StreamSuccessCode.REPLAY_WATCH_SUCCESS));
    }

    /*
     * 다시보기 HLS 매니페스트. watchReplay 응답의 playbackUrl이 이 엔드포인트를 가리킨다.
     * 플레이어(hls.js 등)가 직접 파싱하는 리소스라 SuccessResponse로 감싸지 않는다.
     */
    @GetMapping(value = "/{liveId}/replay/playlist", produces = "application/vnd.apple.mpegurl")
    public String getReplayPlaylist(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        return streamReplayService.buildReplayPlaylist(liveId);
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

    // 다시보기 목록 조회 (팔로우 탭, 최신순/인기순)
    @GetMapping("/replays/following")
    public ResponseEntity<SuccessResponse<CursorPage<ReplayResponse>>> getFollowingReplays(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size,
            @RequestParam(defaultValue = "LATEST") ReplaySort sort
    ) {
        CursorPage<ReplayResponse> page = streamReplayService.getFollowingReplays(
                authMember.getUser().getId(), cursor, size, sort
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(page, StreamSuccessCode.REPLAY_LIST_SUCCESS));
    }

    // 다시보기 목록 조회 (전체 탭, 최신순/인기순)
    @GetMapping("/replays/all")
    public ResponseEntity<SuccessResponse<CursorPage<ReplayResponse>>> getAllReplays(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size,
            @RequestParam(defaultValue = "LATEST") ReplaySort sort
    ) {
        CursorPage<ReplayResponse> page = streamReplayService.getAllReplays(cursor, size, sort);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(page, StreamSuccessCode.REPLAY_LIST_SUCCESS));
    }

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
    public ResponseEntity<SuccessResponse<Void>> reportUser(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId,
            @Valid @RequestBody ReportUserRequest request
    ) {
        streamService.reportUser(authMember.getUser(), liveId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new SuccessResponse<>(null, StreamSuccessCode.LIVE_REPORT_SUCCESS));
    }

    @GetMapping("/{liveId}/reservation")
    public ResponseEntity<SuccessResponse<ReservationEditResponse>> getReservationForEdit(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        ReservationEditResponse response = streamService.getReservationForEdit(
                authMember.getUser(), liveId
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(response, StreamSuccessCode.RESERVATION_EDIT_VIEW_SUCCESS));
    }

    @PatchMapping("/{liveId}/reservation")
    public ResponseEntity<SuccessResponse<Void>> updateReservation(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId,
            @Valid @RequestBody ReservationPatchRequest request
    ) {
        streamService.updateReservation(authMember.getUser(), liveId, request);
        SuccessResponse<Void> body = new SuccessResponse<>(null, StreamSuccessCode.RESERVATION_UPDATE_SUCCESS);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @DeleteMapping("/{liveId}/reservation")
    public ResponseEntity<SuccessResponse<Void>> cancelReservation(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId
    ) {
        streamService.cancelReservation(authMember.getUser(), liveId);
        SuccessResponse<Void> body = new SuccessResponse<>(null, StreamSuccessCode.RESERVATION_CANCEL_SUCCESS);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    // watchOnly=true: 홈 화면 등 방 밖에서 카운트 수신 전용 구독 (시청자 수에 미포함)
    @GetMapping(value = "/{liveId}/viewers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeViewer(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long liveId,
            @RequestParam(defaultValue = "false") boolean watchOnly,
            HttpServletResponse response
    ) {
        // 리버스 프록시(nginx 등)가 이 SSE 응답만 버퍼링하지 않도록(실시간 전송)
        response.setHeader("X-Accel-Buffering", "no");
        return streamService.subscribeViewerCount(authMember.getUser().getId(), liveId, watchOnly);
    }
}
