package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.LiveAlarmToggleResponse;
import com.umc.bscene.domain.stream.dto.response.LiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.dto.response.StreamSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.UpcomingLiveResponse;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.response.CursorPage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

public interface StreamService {

    // 인증, 인가
    Boolean canPublish(String accessToken, String path);
    Boolean canRead(String accessToken, String path);

    // 방송 시작, 종료
    StreamCreateResponse createStream(User user, StreamCreateRequest request);
    void closeStream(Long userId, Long streamId);

    // 방송 종료 화면
    StreamSummaryResponse getStreamSummary(Long userId, Long liveId);

    // 방송 상태
    CursorPage<LiveStreamResponse> getLiveStreams(Long cursor, int size);

    void syncLiveState(Set<String> readyPaths);

    // 방 진입 (송출자, 청취자 모두 이 메소드에서 처리
    StreamRoomResponse enterRoom(Long userId, Long liveId);

    void leaveRoom(Long userId, Long liveId);

    SseEmitter subscribeViewerCount(Long userId, Long liveId);

    // 라이브 홈 통합 조회 (currentMode에 따라 팬/밴드 분기)
    LiveHomeResponse getLiveHome(User user);

    // 팔로우한 밴드의 라이브 중 목록 (팬 모드 전용, 유저별 데이터라 캐싱 X)
    CursorPage<LiveStreamResponse> getFollowingLiveStreams(User user, Long cursor, int size);

    // 예정된 라이브 목록 (나의 알림 설정 여부 포함 → 유저별 데이터라 캐싱 X)
    CursorPage<UpcomingLiveResponse> getUpcomingLives(User user, boolean following, Long cursor, int size);

    // 예정된 라이브 알림 설정 토글
    LiveAlarmToggleResponse toggleLiveAlarm(User user, Long liveId);
}
