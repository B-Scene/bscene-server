package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.ReplayFirstPostedAt;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
 * S3에 업로드된 다시보기를 라이브 단위로, 첫 게시 시각(첫 세그먼트 행 생성 시각)으로부터 72시간이 지나면 전부 삭제한다.
 * 스케줄러가 1시간마다 스캔해 다음 1시간 안에 만료되는 라이브만 TaskScheduler에 만료 시각으로 예약하고,
 * 그보다 뒤에 만료되는 라이브는 다음 스캔에 맡긴다.
 */
@Slf4j
@RequiredArgsConstructor
public class ReplayExpirationService {

    // 라이브의 첫 게시 시각 기준 보관 기간
    static final Duration RETENTION = Duration.ofHours(72);

    // 스캔 주기와 같은 길이. 이 구간 안에 만료되는 라이브만 예약한다
    static final Duration LOOKAHEAD = Duration.ofHours(1);

    private final StreamReplayRepository streamReplayRepository;
    private final S3Client s3Client;
    private final TaskScheduler taskScheduler;
    private final String bucket;

    // createdAt은 JPA Auditing이 시스템 기본 타임존의 LocalDateTime.now()로 채우므로 같은 타임존으로 계산해야 한다
    private final Clock clock;

    // 예약 후 아직 실행이 끝나지 않은 라이브(audioStream) id. 스캔 구간 경계나 실행 지연으로 같은 라이브가 중복 예약되는 것을 막는다
    private final Set<Long> reservedAudioStreamIds = ConcurrentHashMap.newKeySet();

    public void reserveExpiringReplays() {
        LocalDateTime now = LocalDateTime.now(clock);

        // firstPostedAt + 72h <= now + 1h  ⇔  firstPostedAt <= now + 1h - 72h
        LocalDateTime threshold = now.plus(LOOKAHEAD).minus(RETENTION);
        List<ReplayFirstPostedAt> targets = streamReplayRepository.findFirstPostedAtOnOrBefore(threshold);

        int reserved = 0;
        for (ReplayFirstPostedAt target : targets) {
            if (reserve(target.audioStreamId(), target.firstPostedAt().plus(RETENTION)))
                reserved++;
        }

        log.info("다시보기 만료 스캔 완료: 대상 라이브 {}건, 신규 예약 {}건", targets.size(), reserved);
    }

    private boolean reserve(Long audioStreamId, LocalDateTime expiresAt) {
        // 이미 예약되어 실행을 기다리는 라이브는 건너뜀
        if (!reservedAudioStreamIds.add(audioStreamId))
            return false;

        // 만료 시각이 이미 지난 라이브(삭제 실패 후 재시도, 재시작으로 유실된 예약)는 과거 시각으로 예약되어 즉시 실행된다
        Instant runAt = expiresAt.atZone(clock.getZone()).toInstant();

        try {
            taskScheduler.schedule(() -> deleteExpired(audioStreamId), runAt);
            return true;
        } catch (RuntimeException e) {
            // 종료 중인 스케줄러(TaskRejectedException) 등으로 예약 불가 → 예약 표시를 풀어 다음 스캔에서 다시 시도.
            // 예외 종류와 관계없이 풀어야 id가 집합에 남아 재기동 전까지 영영 재예약되지 않는 일이 없다
            reservedAudioStreamIds.remove(audioStreamId);
            log.warn("다시보기 만료 삭제 예약 실패 audioStreamId={}", audioStreamId, e);
            return false;
        }
    }

    /*
     * 예약 시각에 TaskScheduler 스레드에서 실행된다. 실행 시점에 라이브의 세그먼트를 다시 조회해 전부 지운다.
     * 모든 세그먼트의 S3 객체를 먼저 지운 뒤 DB 행을 한 번에 지운다.
     * - S3 → DB 순서: 행을 먼저 지우면 S3 삭제 실패 시 객체를 다시 찾을 단서가 사라져 영구히 남는다
     * - 행 일괄 삭제: S3 삭제가 중간에 실패하면 행을 하나도 지우지 않으므로, 다음 스캔에서도 첫 게시 시각이 그대로 유지된 채 재시도된다
     * S3 삭제는 대상이 없어도 성공하므로 재실행에 안전하다.
     */
    void deleteExpired(Long audioStreamId) {
        try {
            List<StreamReplay> segments = streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(audioStreamId);
            if (segments.isEmpty())
                return;

            for (StreamReplay segment : segments) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(segment.getS3Key())
                        .build());
            }

            streamReplayRepository.deleteAllByIdInBatch(segments.stream().map(StreamReplay::getId).toList());
            log.info("보관 기간이 지난 다시보기 삭제 완료 audioStreamId={}, 세그먼트 {}건", audioStreamId, segments.size());
        } catch (Exception e) {
            log.warn("다시보기 만료 삭제 실패, 다음 스캔에서 재시도함 audioStreamId={}", audioStreamId, e);
        } finally {
            reservedAudioStreamIds.remove(audioStreamId);
        }
    }
}
