package com.umc.bscene.domain.stream.sse;

import com.umc.bscene.domain.stream.dto.response.CoHostUpgradeEvent;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * 시청자 수 SSE의 구독·브로드캐스트·하트비트·유령 정리를 담당한다.
 * <p>
 * 프레젠스는 viewer:{liveId} ZSet(member = userId, score = epoch초)로 관리한다.
 * 하트비트가 살아있는 시청자의 score를 갱신하고, 갱신되지 않은(끊긴/유령) 시청자는 sweep이 제거한다.
 * 따라서 브라우저를 그냥 닫아 leaveRoom이 호출되지 않아도 최대 STALE_SECONDS 안에 카운트에서 빠진다.
 */
@RequiredArgsConstructor
public class ViewerSsePresence {

    private final ViewerSseRegistry registry;
    private final StringRedisTemplate redisTemplate;
    private final AudioStreamRepository audioStreamRepository;

    private static final String VIEWER_KEY_PREFIX = "viewer:";
    private static final long HEARTBEAT_MS = 15_000L;
    // 반드시 하트비트 주기(15s)보다 넉넉히 커야 한다. 비트 사이에 살아있는 시청자가 잘리는 것을 방지(3비트 허용).
    private static final long STALE_SECONDS = 45L;

    /** 시청자 수 SSE 구독. OPEN 방만 허용한다. watchOnly=true면 카운트 수신만 하고 시청자 수에는 포함되지 않는다. */
    public SseEmitter subscribe(Long userId, Long liveId, boolean watchOnly) {
        AudioStream stream = audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));
        if (stream.getStatus() != StreamStatus.OPEN)
            throw new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

        // 보기 전용(홈 화면 등 방 밖 구독): 프레젠스 미등록, 유저당 1연결 제한 미적용
        if (watchOnly) {
            SseEmitter emitter = registry.registerWatchOnly(liveId);
            broadcastCount(liveId); // 구독 직후 현재 카운트 전송
            return emitter;
        }

        // 송출자는 시청자 수에 포함하지 않는다(카운트 업데이트는 수신하되 본인은 프레젠스 미포함)
        boolean counted = !stream.getBroadcasterId().equals(userId);

        if (counted) touch(liveId, userId); // 프레젠스 등록(score = now)

        SseEmitter emitter = registry.register(liveId, userId, counted, () -> {
            // 이 유저의 마지막 연결이 끊기면 프레젠스 제거 + 카운트 반영
            if (counted)
                redisTemplate.opsForZSet().remove(VIEWER_KEY_PREFIX + liveId, String.valueOf(userId));
            broadcastCount(liveId);
        });

        broadcastCount(liveId); // 새 구독자를 포함한 전원에게 현재 카운트 전송
        return emitter;
    }

    /** liveId 방의 현재 시청자 수를 전 구독자에게 브로드캐스트한다. */
    public void broadcastCount(Long liveId) {
        registry.broadcast(liveId, currentCount(liveId));
    }

    /**
     * 새 진행자 합류를 기존 진행자들에게만 알린다(coPublisherJoined 이벤트).
     * payload에 멤버 path(WHEP URL)가 담기므로 브로드캐스트가 아닌 타겟 전송만 사용한다.
     */
    public void notifyCoPublisherJoined(
            Long liveId,
            Collection<Long> targetUserIds,
            StreamRoomResponse.CoPublisher joined
    ) {
        if (targetUserIds.isEmpty()) return;

        registry.sendToUsers(liveId, targetUserIds, "coPublisherJoined", joined);
    }

    /*
     * 공동 송출자 업그레이드 요청을 송출자에게만 알린다(coHostUpgradeRequested).
     * 송출자는 방송 중 이탈이 불가하므로, FE는 이 이벤트로 수락 모달을 띄운다
     */
    public void notifyCoHostUpgradeRequested(Long liveId, Long broadcasterId, CoHostUpgradeEvent event) {
        registry.sendToUsers(liveId, List.of(broadcasterId), "coHostUpgradeRequested", event);
    }

    /* 업그레이드 수락 결과를 요청자에게만 알린다(coHostUpgradeAccepted). 수신 시 FE가 enterRoom을 재호출한다 */
    public void notifyCoHostUpgradeAccepted(Long liveId, Long requesterId, CoHostUpgradeEvent event) {
        registry.sendToUsers(liveId, List.of(requesterId), "coHostUpgradeAccepted", event);
    }

    private long currentCount(Long liveId) {
        Long count = redisTemplate.opsForZSet().zCard(VIEWER_KEY_PREFIX + liveId);
        return count == null ? 0 : count;
    }

    private void touch(Long liveId, Long userId) {
        redisTemplate.opsForZSet().add(
                VIEWER_KEY_PREFIX + liveId, String.valueOf(userId), Instant.now().getEpochSecond());
    }

    /** 하트비트: 살아있는 연결에 ping을 보내고, 그 시청자의 프레젠스 score를 갱신한다. */
    @Scheduled(fixedRate = HEARTBEAT_MS)
    public void heartbeat() {
        registry.pingAndCollectAlive(this::touch);
    }

    /** 유령 정리: score가 갱신되지 않은(끊긴) 시청자를 ZSet에서 제거하고 카운트를 반영한다. */
    @Scheduled(fixedRate = HEARTBEAT_MS)
    public void sweepStale() {
        long cutoff = Instant.now().getEpochSecond() - STALE_SECONDS;
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(VIEWER_KEY_PREFIX + "*").count(200).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
                if (removed != null && removed > 0) {
                    Long liveId = Long.valueOf(key.substring(VIEWER_KEY_PREFIX.length()));
                    broadcastCount(liveId);
                }
            }
        }
    }
}
