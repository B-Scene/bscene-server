package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.response.CursorPage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

public interface StreamService {

    // 인증, 인가
    Boolean canPublish(String accessToken, String path);
    Boolean canRead(String accessToken, String path);

    // 방송 시작, 종료
    StreamCreateResponse createStream(User user, Long userId, StreamCreateRequest request);
    void closeStream(Long userId, String path);

    // 방송 상태
    CursorPage<LiveStreamResponse> getLiveStreams(Long cursor, int size);

    void syncLiveState(Set<String> readyPaths);

    // 방 진입 (송출자, 청취자 모두 이 메소드에서 처리
    StreamRoomResponse enterRoom(Long userId, Long liveId);

    void leaveRoom(Long userId, Long liveId);

    SseEmitter subscribeViewerCount(Long userId, Long liveId);
}
