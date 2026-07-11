package com.umc.bscene.domain.stream.controller;

import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamReplayResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.dto.response.StreamSummaryResponse;
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
            @Valid @RequestBody StreamCreateRequest request
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
