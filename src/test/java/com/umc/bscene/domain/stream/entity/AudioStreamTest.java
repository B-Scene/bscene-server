package com.umc.bscene.domain.stream.entity;

import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AudioStream의 상태 전이 규칙 검증.
 * <p>
 * 아래 불변식은 스케줄러/서비스 어느 경로에서 호출되든 유지되어야 한다:
 * - markStarted()는 재송출 시에도 최초 시작 시각(startedAt)을 덮어쓰지 않는다
 * - cancel()은 SCHEDULED에서만 동작한다 (이미 시작/종료된 방을 예약 취소로 덮지 않는다)
 * - close()는 종료 시점 시청자 수를 스냅샷으로 그대로 보관한다
 */
@DisplayName("AudioStream 상태 전이")
class AudioStreamTest {

    @Nested
    @DisplayName("close()")
    class Close {

        @Test
        @DisplayName("상태를 CLOSED로 바꾸고 종료 시각과 시청자 수 스냅샷을 남긴다")
        void closesStreamWithViewerSnapshot() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            LocalDateTime before = LocalDateTime.now();

            audioStream.close(37);

            LocalDateTime after = LocalDateTime.now();
            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(audioStream.getClosedViewerCount()).isEqualTo(37);
            assertThat(audioStream.getClosedAt()).isNotNull();
            assertThat(audioStream.getClosedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("시청자 0명도 null이 아닌 0으로 저장한다")
        void storesZeroViewerCountVerbatim() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);

            audioStream.close(0);

            assertThat(audioStream.getClosedViewerCount()).isNotNull();
            assertThat(audioStream.getClosedViewerCount()).isZero();
        }

        @Test
        @DisplayName("큰 시청자 수도 가공 없이 그대로 저장한다")
        void storesLargeViewerCountVerbatim() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);

            audioStream.close(Integer.MAX_VALUE);

            assertThat(audioStream.getClosedViewerCount()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("이미 CLOSED인 방에 다시 호출하면 스냅샷과 종료 시각을 덮어쓴다 (별도 가드 없음)")
        void overwritesSnapshotWhenAlreadyClosed() {
            LocalDateTime originalStartedAt = LocalDateTime.now().minusHours(2);
            LocalDateTime originalClosedAt = LocalDateTime.now().minusHours(1);
            AudioStream audioStream = StreamFixtures.closedStream(1L, 10L, 100L, originalStartedAt, originalClosedAt, 5);

            audioStream.close(99);

            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(audioStream.getClosedViewerCount()).isEqualTo(99);
            assertThat(audioStream.getClosedAt()).isAfter(originalClosedAt);
            assertThat(audioStream.getStartedAt()).isEqualTo(originalStartedAt);
        }

        @Test
        @DisplayName("SCHEDULED 방도 곧바로 CLOSED로 전이시킨다")
        void closesScheduledStreamDirectly() {
            AudioStream audioStream = StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1));

            audioStream.close(3);

            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(audioStream.getClosedViewerCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("markStarted()")
    class MarkStarted {

        @Test
        @DisplayName("상태를 OPEN으로 바꾸고 최초 시작 시각을 기록한다")
        void marksOpenAndRecordsStartedAt() {
            AudioStream audioStream = StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusMinutes(10));
            assertThat(audioStream.getStartedAt()).isNull();
            LocalDateTime before = LocalDateTime.now();

            audioStream.markStarted();

            LocalDateTime after = LocalDateTime.now();
            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.OPEN);
            assertThat(audioStream.getStartedAt()).isNotNull();
            assertThat(audioStream.getStartedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("재송출 시 기존 startedAt을 덮어쓰지 않고 상태만 OPEN으로 되돌린다")
        void preservesOriginalStartedAtOnRebroadcast() {
            LocalDateTime originalStartedAt = LocalDateTime.now().minusHours(3);
            LocalDateTime originalClosedAt = LocalDateTime.now().minusHours(2);
            AudioStream audioStream = StreamFixtures.closedStream(1L, 10L, 100L, originalStartedAt, originalClosedAt, 12);

            audioStream.markStarted();

            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.OPEN);
            assertThat(audioStream.getStartedAt()).isEqualTo(originalStartedAt);
        }

        @Test
        @DisplayName("연속 호출해도 최초 시작 시각이 유지된다")
        void isIdempotentForStartedAt() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.SCHEDULED);

            audioStream.markStarted();
            LocalDateTime firstStartedAt = audioStream.getStartedAt();
            audioStream.markStarted();
            audioStream.markStarted();

            assertThat(audioStream.getStartedAt()).isEqualTo(firstStartedAt);
            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("SCHEDULED 방만 CANCELED로 전이하며 종료 시각을 남긴다")
        void cancelsScheduledStream() {
            AudioStream audioStream = StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1));
            LocalDateTime before = LocalDateTime.now();

            audioStream.cancel();

            LocalDateTime after = LocalDateTime.now();
            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.CANCELED);
            assertThat(audioStream.getClosedAt()).isNotNull();
            assertThat(audioStream.getClosedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("이미 시작된(OPEN) 방은 취소되지 않는다")
        void doesNotCancelOpenStream() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);

            audioStream.cancel();

            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.OPEN);
            assertThat(audioStream.getClosedAt()).isNull();
        }

        @Test
        @DisplayName("이미 종료된(CLOSED) 방은 취소되지 않고 종료 시각도 그대로다")
        void doesNotCancelClosedStream() {
            LocalDateTime originalStartedAt = LocalDateTime.now().minusHours(2);
            LocalDateTime originalClosedAt = LocalDateTime.now().minusHours(1);
            AudioStream audioStream = StreamFixtures.closedStream(1L, 10L, 100L, originalStartedAt, originalClosedAt, 8);

            audioStream.cancel();

            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(audioStream.getClosedAt()).isEqualTo(originalClosedAt);
            assertThat(audioStream.getClosedViewerCount()).isEqualTo(8);
        }

        @Test
        @DisplayName("이미 CANCELED인 방에 다시 호출해도 아무것도 바뀌지 않는다")
        void isNoOpWhenAlreadyCanceled() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CANCELED);

            audioStream.cancel();

            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.CANCELED);
            assertThat(audioStream.getClosedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("markMemberReminderSent()")
    class MarkMemberReminderSent {

        @Test
        @DisplayName("전달받은 발송 시각을 그대로 보관한다")
        void storesSentAt() {
            AudioStream audioStream = StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusMinutes(30));
            assertThat(audioStream.getMemberReminderSentAt()).isNull();
            LocalDateTime sentAt = LocalDateTime.of(2026, 7, 26, 13, 45, 30);

            audioStream.markMemberReminderSent(sentAt);

            assertThat(audioStream.getMemberReminderSentAt()).isEqualTo(sentAt);
        }

        @Test
        @DisplayName("상태나 예약 시각에는 영향을 주지 않는다")
        void doesNotTouchOtherFields() {
            LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
            AudioStream audioStream = StreamFixtures.scheduledStream(1L, 10L, 100L, scheduledAt);

            audioStream.markMemberReminderSent(LocalDateTime.now());

            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.SCHEDULED);
            assertThat(audioStream.getScheduledAt()).isEqualTo(scheduledAt);
        }
    }

    @Nested
    @DisplayName("markStarted() -> close() 조합")
    class StartThenClose {

        @Test
        @DisplayName("방송 시간 계산에 필요한 startedAt/closedAt이 모두 채워진다")
        void leavesBothTimestampsForDurationCalculation() {
            AudioStream audioStream = StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusMinutes(5));

            audioStream.markStarted();
            audioStream.close(21);

            assertThat(audioStream.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(audioStream.getStartedAt()).isNotNull();
            assertThat(audioStream.getClosedAt()).isNotNull();
            assertThat(audioStream.getClosedAt()).isAfterOrEqualTo(audioStream.getStartedAt());
            assertThat(audioStream.getClosedViewerCount()).isEqualTo(21);
        }
    }
}
