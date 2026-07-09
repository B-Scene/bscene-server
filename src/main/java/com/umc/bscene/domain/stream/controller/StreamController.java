package com.umc.bscene.domain.stream.controller;

import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.enums.code.success.StreamSuccessCode;
import com.umc.bscene.domain.stream.service.StreamService;
import com.umc.bscene.global.response.ApiResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/live")
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
}
