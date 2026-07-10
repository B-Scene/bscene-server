package com.umc.bscene.domain.stream.controller;

import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.enums.code.success.StreamSuccessCode;
import com.umc.bscene.domain.stream.service.StreamService;
import com.umc.bscene.global.response.ApiResponse;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/lives")
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
        SuccessResponse<Void> body = (SuccessResponse<Void>) SuccessResponse.of(null, StreamSuccessCode.LIVE_ROOM_LEAVE_SUCCESS);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<CursorPage<LiveStreamResponse>>> getInLiveStreams(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
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
        SuccessResponse<Void> response = (SuccessResponse<Void>) SuccessResponse.of(null, StreamSuccessCode.LIVE_CLOSE_SUCCESS);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
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
