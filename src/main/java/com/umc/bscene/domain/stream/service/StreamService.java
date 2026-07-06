package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;

import java.util.List;
import java.util.Set;

public interface StreamService {

    // 인증, 인가
    Boolean canPublish(String accessToken, String path);
    Boolean canRead(String accessToken, String path);

    // 방송 시작, 종료
    StreamCreateResponse createStream(Long userId, StreamCreateRequest request);
    void closeStream(Long userId, String path);

    // 방송 상태
    List<LiveStreamResponse> getLiveStreams();
    void syncLiveState(Set<String> readyPath);
}
