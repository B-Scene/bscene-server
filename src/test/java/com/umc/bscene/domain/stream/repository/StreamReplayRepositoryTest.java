package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.dto.ReplayFirstPostedAt;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(JpaAuditingConfig.class)
@DisplayName("StreamReplayRepository")
class StreamReplayRepositoryTest {

    private static final LocalDateTime THRESHOLD = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Autowired
    private StreamReplayRepository streamReplayRepository;

    @Autowired
    private AudioStreamRepository audioStreamRepository;

    @Autowired
    private EntityManager entityManager;

    private AudioStream saveStream(String path) {
        return audioStreamRepository.save(AudioStream.builder()
                .broadcasterId(1L)
                .bandId(1L)
                .path(path)
                .title("title-" + path)
                .status(StreamStatus.CLOSED)
                .build());
    }

    private void saveSegment(AudioStream stream, String s3Key, LocalDateTime createdAt) {
        StreamReplay replay = streamReplayRepository.saveAndFlush(StreamReplay.builder()
                .audioStream(stream)
                .s3Key(s3Key)
                .durationSec(60)
                .viewCount(0L)
                .build());

        // createdAt은 Auditing이 채우고 updatable = false라 엔티티로 바꿀 수 없어 네이티브 쿼리로 덮어쓴다
        entityManager.createNativeQuery(
                        "update stream_replay set created_at = :createdAt where stream_replay_id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", replay.getId())
                .executeUpdate();
    }

    @Nested
    @DisplayName("findFirstPostedAtOnOrBefore")
    class FindFirstPostedAtOnOrBefore {

        @Test
        @DisplayName("라이브별 가장 이른 세그먼트 createdAt을 첫 게시 시각으로, 기준 이하인 라이브만 라이브당 한 건씩 반환한다")
        void returnsEarliestSegmentPerLiveOnOrBeforeThreshold() {
            // 첫 세그먼트가 기준 이전 → 늦게 올라온 세그먼트가 있어도 라이브 전체가 대상
            AudioStream expired = saveStream("expired");
            saveSegment(expired, "recordings/expired/b.mp4", THRESHOLD.plusHours(5));
            saveSegment(expired, "recordings/expired/a.mp4", THRESHOLD.minusHours(2));

            // 첫 세그먼트가 정확히 기준 시각 → 경계 포함
            AudioStream boundary = saveStream("boundary");
            saveSegment(boundary, "recordings/boundary/a.mp4", THRESHOLD);

            // 모든 세그먼트가 기준 이후 → 제외
            AudioStream notYet = saveStream("not-yet");
            saveSegment(notYet, "recordings/not-yet/a.mp4", THRESHOLD.plusMinutes(1));
            saveSegment(notYet, "recordings/not-yet/b.mp4", THRESHOLD.plusMinutes(10));

            List<ReplayFirstPostedAt> result = streamReplayRepository.findFirstPostedAtOnOrBefore(THRESHOLD);

            assertThat(result)
                    .extracting(ReplayFirstPostedAt::audioStreamId, ReplayFirstPostedAt::firstPostedAt)
                    .containsExactlyInAnyOrder(
                            tuple(expired.getId(), THRESHOLD.minusHours(2)),
                            tuple(boundary.getId(), THRESHOLD)
                    );
        }

        @Test
        @DisplayName("다시보기가 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoReplays() {
            saveStream("no-replay");

            assertThat(streamReplayRepository.findFirstPostedAtOnOrBefore(THRESHOLD)).isEmpty();
        }
    }
}
