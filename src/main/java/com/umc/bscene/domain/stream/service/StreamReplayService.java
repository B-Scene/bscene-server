package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.StreamReplayResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;

import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StreamReplayService {

    // 다시보기 재생 URL이 오래 열려있지 않도록 짧게 제한
    private static final Duration PLAYBACK_URL_EXPIRATION = Duration.ofMinutes(10);

    private final StreamReplayRepository streamReplayRepository;
    private final AudioStreamRepository audioStreamRepository;
    private final RecordingUploadService recordingUploadService;
    private final BandMemberPort bandMemberPort;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    /*
     * 방송 종료 화면에서 방송자가 "저장" 선택 시 호출.
     * 검증(권한·상태·녹화 파일 존재) 후 세그먼트별 비동기 업로드를 트리거하고 202를 반환.
     * 실패한 업로드는 RecordingUploadSweeper가 pending 키 기반으로 재시도.
     */
    @Transactional(readOnly = true)
    public void requestReplayUpload(Long userId, Long liveId) {
        AudioStream audioStream = audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

        if (!audioStream.getBroadcasterId().equals(userId))
            throw new StreamException(StreamErrorCode.FORBIDDEN_REQUEST);

        if (audioStream.getStatus() != StreamStatus.CLOSED)
            throw new StreamException(StreamErrorCode.STREAM_NOT_CLOSED);

        List<Path> segments = recordingUploadService.findSegments(audioStream.getPath());
        if (segments.isEmpty())
            throw new StreamException(StreamErrorCode.RECORDING_NOT_FOUND);

        recordingUploadService.markPending(audioStream.getPath());

        // RecordingUploadService 빈을 통해 호출해야 @Async 프록시가 적용됨 (self-invocation 방지)
        for (Path segment : segments)
            recordingUploadService.uploadAsync(audioStream.getPath(), segment.toString());
    }

    @Transactional
    public StreamReplayResponse watchReplay(Long liveId) {

        // 라이브의 첫(대표) 세그먼트 조회
        StreamReplay replay = streamReplayRepository.findFirstByAudioStream_IdOrderByCreatedAtAsc(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.REPLAY_NOT_FOUND));

        // S3에 접속(재생)하면 +1. 원자적 증가로 lost update 방지
        streamReplayRepository.increaseViewCount(replay.getId());

        AudioStream audioStream = replay.getAudioStream();

        // 밴드 정보
        BandInfoForGetLiveResponse.BandInfo band = bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(audioStream.getBroadcasterId()))
                .stream().findFirst()
                .map(BandInfoForGetLiveResponse::bandInfo)
                .orElse(null);

        // s3Key에 대한 presigned GET URL 발급
        String playbackUrl = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(PLAYBACK_URL_EXPIRATION)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(replay.getS3Key())
                                .build())
                        .build())
                .url()
                .toString();

        return new StreamReplayResponse(
                audioStream.getTitle(),
                band != null ? band.bandName() : "",
                band != null ? band.bandProfileImageUrl() : "",
                replay.getViewCount() + 1,   // 방금 증가시킨 값을 응답에 반영
                replay.getDurationSec(),
                playbackUrl
        );
    }
}
