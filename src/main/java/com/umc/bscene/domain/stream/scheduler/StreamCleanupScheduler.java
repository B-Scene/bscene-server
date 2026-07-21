package com.umc.bscene.domain.stream.scheduler;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class StreamCleanupScheduler {

    private final AudioStreamRepository audioStreamRepository;
    private final StringRedisTemplate redisTemplate;

    private static final Duration GRACE = Duration.ofSeconds(30);
    private static final String LIVE_KEY_PREFIX = "live:";

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelAbandonedScheduled() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minus(GRACE);

        // 조건부 벌크 UPDATE라 스케줄러가 엔티티를 읽은 사이 송출자가 진입(OPEN 전환)해도 덮어쓰지 않는다
        // (status = SCHEDULED 조건이 DB에서 원자적으로 검사됨)
        audioStreamRepository.cancelAbandonedScheduled(threshold, now);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void closeAbandonedOpen() {
        LocalDateTime threshold = LocalDateTime.now().minus(GRACE);
        List<AudioStream> candidates = audioStreamRepository.findByStatusAndStartedAtBefore(
                StreamStatus.OPEN, threshold
        );

        for (AudioStream audioStream : candidates) {
            boolean live = Boolean.TRUE.equals(redisTemplate.hasKey(LIVE_KEY_PREFIX + audioStream.getPath()));
            if (!live) {
                Long count = redisTemplate.opsForZSet().zCard("viewer:" + audioStream.getId());
                audioStream.close(count != null ? count.intValue() : 0);
            }
        }
    }
}
