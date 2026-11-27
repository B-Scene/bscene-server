package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.ReplayFirstPostedAt;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 다시보기 72시간 만료 삭제 검증.
 * <p>
 * 핵심 불변식:
 * - 만료 기준은 라이브의 첫 게시 시각(세그먼트 createdAt 최솟값)이며, 라이브 단위로 firstPostedAt + 72h에 예약한다
 * - 스캔은 "다음 1시간 안에 만료되는" 라이브까지만 조회하고, 실행 대기 중인 라이브는 중복 예약하지 않는다
 * - 실행 시 라이브의 세그먼트를 전부 지우며, 모든 S3 삭제가 끝난 뒤에만 DB 행을 일괄 삭제한다
 * - 실패하면 예약 표시를 풀어 다음 스캔에서 재시도할 수 있다
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReplayExpirationService")
class ReplayExpirationServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T03:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final Long LIVE_ID = 10L;

    @Mock
    private StreamReplayRepository streamReplayRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private TaskScheduler taskScheduler;

    private ReplayExpirationService service;

    @BeforeEach
    void setUp() {
        service = new ReplayExpirationService(streamReplayRepository, s3Client, taskScheduler, BUCKET, CLOCK);
    }

    private void givenExpiringLive(LocalDateTime firstPostedAt) {
        when(streamReplayRepository.findFirstPostedAtOnOrBefore(any()))
                .thenReturn(List.of(new ReplayFirstPostedAt(LIVE_ID, firstPostedAt)));
    }

    private void givenSegments() {
        when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(LIVE_ID)).thenReturn(List.of(
                StreamFixtures.replay(1L, null, "recordings/live/a.mp4", 60, 0L),
                StreamFixtures.replay(2L, null, "recordings/live/b.mp4", 60, 0L)
        ));
    }

    private Instant instantOf(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant();
    }

    private Runnable captureScheduledTask() {
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(taskCaptor.capture(), any(Instant.class));
        return taskCaptor.getValue();
    }

    @Nested
    @DisplayName("스캔 및 예약")
    class Reserve {

        @Test
        @DisplayName("첫 게시 시각이 now + 1h - 72h 이하인 라이브를 조회한다 (다음 1시간 안에 만료되는 라이브)")
        void queriesLivesExpiringWithinNextHour() {
            when(streamReplayRepository.findFirstPostedAtOnOrBefore(any())).thenReturn(List.of());

            service.reserveExpiringReplays();

            verify(streamReplayRepository).findFirstPostedAtOnOrBefore(NOW.plusHours(1).minusHours(72));
            verifyNoInteractions(taskScheduler);
        }

        @Test
        @DisplayName("라이브를 첫 게시 시각 + 72h 시각으로 TaskScheduler에 예약한다")
        void schedulesLiveAtFirstPostedAtPlusRetention() {
            LocalDateTime firstPostedAt = NOW.minusHours(72).plusMinutes(30);   // 30분 뒤 만료
            givenExpiringLive(firstPostedAt);

            service.reserveExpiringReplays();

            verify(taskScheduler).schedule(any(Runnable.class), eq(instantOf(firstPostedAt.plusHours(72))));
        }

        @Test
        @DisplayName("이미 만료 시각이 지난 라이브는 과거 시각으로 예약되어 즉시 실행된다")
        void overdueLiveIsScheduledInThePast() {
            givenExpiringLive(NOW.minusHours(80));

            service.reserveExpiringReplays();

            ArgumentCaptor<Instant> runAtCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(taskScheduler).schedule(any(Runnable.class), runAtCaptor.capture());
            assertThat(runAtCaptor.getValue()).isBefore(CLOCK.instant());
        }

        @Test
        @DisplayName("실행 대기 중인 라이브는 다음 스캔에서 중복 예약하지 않는다")
        void doesNotReserveTwiceWhilePending() {
            givenExpiringLive(NOW.minusHours(72));

            service.reserveExpiringReplays();
            service.reserveExpiringReplays();

            verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        @DisplayName("예약이 거절되면 예약 표시를 풀어 다음 스캔에서 다시 예약한다")
        void releasesReservationWhenRejected() {
            givenExpiringLive(NOW.minusHours(72));
            when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenThrow(new TaskRejectedException("shutdown"))
                    .thenReturn(null);

            service.reserveExpiringReplays();
            service.reserveExpiringReplays();

            verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("예약된 삭제 실행")
    class Delete {

        @Test
        @DisplayName("라이브의 모든 세그먼트 S3 객체를 지운 뒤 DB 행을 일괄 삭제한다")
        void deletesAllSegmentsS3ThenRows() {
            givenExpiringLive(NOW.minusHours(72));
            givenSegments();
            service.reserveExpiringReplays();

            captureScheduledTask().run();

            ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            InOrder order = inOrder(s3Client, streamReplayRepository);
            order.verify(s3Client, times(2)).deleteObject(requestCaptor.capture());
            order.verify(streamReplayRepository).deleteAllByIdInBatch(List.of(1L, 2L));

            assertThat(requestCaptor.getAllValues())
                    .extracting(DeleteObjectRequest::bucket, DeleteObjectRequest::key)
                    .containsExactly(
                            tuple(BUCKET, "recordings/live/a.mp4"),
                            tuple(BUCKET, "recordings/live/b.mp4")
                    );
        }

        @Test
        @DisplayName("S3 삭제가 중간에 실패하면 DB 행을 하나도 지우지 않고, 다음 스캔에서 다시 예약된다")
        void keepsAllRowsAndAllowsRetryWhenS3DeleteFails() {
            givenExpiringLive(NOW.minusHours(72));
            givenSegments();
            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenReturn(DeleteObjectResponse.builder().build())
                    .thenThrow(SdkClientException.create("network down"));

            service.reserveExpiringReplays();
            captureScheduledTask().run();

            verify(streamReplayRepository, never()).deleteAllByIdInBatch(anyIterable());

            service.reserveExpiringReplays();
            verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        @DisplayName("실행 시점에 세그먼트가 이미 없으면 아무것도 지우지 않는다")
        void doesNothingWhenSegmentsAlreadyGone() {
            givenExpiringLive(NOW.minusHours(72));
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(LIVE_ID)).thenReturn(List.of());
            service.reserveExpiringReplays();

            captureScheduledTask().run();

            verifyNoInteractions(s3Client);
            verify(streamReplayRepository, never()).deleteAllByIdInBatch(anyIterable());
        }
    }
}
