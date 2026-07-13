package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.ReplayResponse;
import com.umc.bscene.domain.stream.dto.response.StreamReplayResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.enums.ReplaySort;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.FollowPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import com.umc.bscene.global.config.CacheConfig;
import com.umc.bscene.global.response.CursorPage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StreamReplayServiceImpl implements StreamReplayService {

    // presigned URL 만료 여유분. 실제 만료는 총 재생 길이 + 이 값 (뒷 세그먼트 재생 도중 만료 방지)
    private static final Duration PLAYBACK_URL_EXPIRATION_MARGIN = Duration.ofMinutes(10);

    private final StreamReplayRepository streamReplayRepository;
    private final AudioStreamRepository audioStreamRepository;
    private final RecordingUploadService recordingUploadService;
    private final BandMemberPort bandMemberPort;
    private final FollowPort followPort;
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
    @Override
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

    @Override
    @Transactional
    public StreamReplayResponse watchReplay(Long liveId) {

        // 라이브의 전체 세그먼트 (재생 순서). 조회수는 대표(첫) 세그먼트 행에 몰아준다
        List<StreamReplay> segments = streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(liveId);
        if (segments.isEmpty())
            throw new StreamException(StreamErrorCode.REPLAY_NOT_FOUND);

        StreamReplay replay = segments.getFirst();

        // 재생 진입 시 +1. 원자적 증가로 lost update 방지
        streamReplayRepository.increaseViewCount(replay.getId());

        AudioStream audioStream = replay.getAudioStream();

        // 밴드 정보
        BandInfoForGetLiveResponse.BandInfo band = bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(audioStream.getBroadcasterId()))
                .stream().findFirst()
                .map(BandInfoForGetLiveResponse::bandInfo)
                .orElse(null);

        // 재생 URL은 HLS 매니페스트 엔드포인트. 세그먼트가 여러 개여도 플레이어가 이어 재생한다
        int totalDurationSec = segments.stream().mapToInt(StreamReplay::getDurationSec).sum();

        return new StreamReplayResponse(
                audioStream.getTitle(),
                band != null ? band.bandName() : "",
                band != null ? band.bandProfileImageUrl() : "",
                replay.getViewCount() + 1,   // 방금 증가시킨 값을 응답에 반영
                totalDurationSec,
                "/lives/" + liveId + "/replay/playlist"
        );
    }

    /*
     * 다시보기 HLS 매니페스트(m3u8) 생성.
     * 세그먼트별 독립 mp4 파일들을 플레이어가 순서대로 이어 재생하도록 presigned URL을 나열한다.
     * 각 세그먼트가 자체 초기화 파일(init+데이터가 한 파일)이라 세그먼트마다 EXT-X-MAP으로 자기 자신을 지정하고,
     * 파일 간 타임스탬프 불연속을 플레이어에 알리기 위해 EXT-X-DISCONTINUITY로 구분한다.
     */
    @Transactional(readOnly = true)
    public String buildReplayPlaylist(Long liveId) {

        List<StreamReplay> segments = streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(liveId);
        if (segments.isEmpty())
            throw new StreamException(StreamErrorCode.REPLAY_NOT_FOUND);

        // 뒷 세그먼트는 앞 세그먼트를 다 재생한 뒤에야 요청되므로, 총 재생 길이 + 여유만큼 서명 유지
        int totalDurationSec = segments.stream().mapToInt(StreamReplay::getDurationSec).sum();
        Duration expiration = PLAYBACK_URL_EXPIRATION_MARGIN.plusSeconds(totalDurationSec);

        int targetDuration = Math.max(
                segments.stream().mapToInt(StreamReplay::getDurationSec).max().orElse(1), 1);

        StringBuilder playlist = new StringBuilder();
        playlist.append("#EXTM3U\n")
                .append("#EXT-X-VERSION:7\n")
                .append("#EXT-X-TARGETDURATION:").append(targetDuration).append('\n')
                .append("#EXT-X-PLAYLIST-TYPE:VOD\n");

        for (int i = 0; i < segments.size(); i++) {
            StreamReplay segment = segments.get(i);
            String url = presignGetUrl(segment.getS3Key(), expiration);

            if (i > 0)
                playlist.append("#EXT-X-DISCONTINUITY\n");

            playlist.append("#EXT-X-MAP:URI=\"").append(url).append("\"\n")
                    .append("#EXTINF:").append(segment.getDurationSec()).append(".0,\n")
                    .append(url).append('\n');
        }

        playlist.append("#EXT-X-ENDLIST\n");
        return playlist.toString();
    }

    private String presignGetUrl(String s3Key, Duration expiration) {
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(expiration)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(s3Key)
                                .build())
                        .build())
                .url()
                .toString();
    }

    /*
     * 팔로우 밴드 다시보기 목록 조회 (최신순/인기순, 커서 페이징)
     * 유저별 응답이라 캐싱하지 않음
     */
    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReplayResponse> getFollowingReplays(Long userId, Long cursor, int size, ReplaySort sort) {

        // 인기순은 (viewCount, id) 복합 keyset이 필요해 커서 행의 viewCount를 조회
        // 커서 다시보기가 삭제된 경우 빈 페이지로 종료 (중복 페이지 방지, getUpcomingLives와 동일 정책)
        Long cursorViewCount = null;
        if (cursor != null && sort == ReplaySort.POPULAR) {
            cursorViewCount = streamReplayRepository.findById(cursor)
                    .map(StreamReplay::getViewCount)
                    .orElse(null);

            if (cursorViewCount == null)
                return CursorPage.empty();
        }

        List<Long> bandIds = followPort.getFollowingBandIds(userId);
        if (bandIds.isEmpty())
            return CursorPage.empty();

        List<StreamReplay> rows = switch (sort) {
            case LATEST -> streamReplayRepository.findReplayPageLatestByBandIds(bandIds, cursor, PageRequest.ofSize(size + 1));
            case POPULAR -> streamReplayRepository.findReplayPagePopularByBandIds(bandIds, cursorViewCount, cursor, PageRequest.ofSize(size + 1));
        };

        return assemblePage(rows, size);
    }

    // size+1로 조회한 rows를 hasNext 판정·슬라이싱하고 밴드 정보를 붙여 응답으로 조립 (전체/팔로우 탭 공통)
    private CursorPage<ReplayResponse> assemblePage(List<StreamReplay> rows, int size) {
        boolean hasNext = rows.size() > size;
        List<StreamReplay> page = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext ? page.getLast().getId() : null;

        // 송출자 ID를 key로 밴드 정보 매핑
        Set<Long> broadcasterIds = page.stream()
                .map(r -> r.getAudioStream().getBroadcasterId())
                .collect(Collectors.toSet());

        Map<Long, BandInfoForGetLiveResponse.BandInfo> bandInfoMap = broadcasterIds.isEmpty()
                ? Map.of()
                : bandMemberPort.getBandNameWithBandProfileByBroadcasterId(broadcasterIds).stream()
                        .collect(Collectors.toMap(
                                BandInfoForGetLiveResponse::broadcasterId,
                                BandInfoForGetLiveResponse::bandInfo,
                                (a, b) -> a
                        ));

        return CursorPage.of(
                page.stream()
                        .map(r -> {
                            BandInfoForGetLiveResponse.BandInfo band = bandInfoMap.get(r.getAudioStream().getBroadcasterId());

                            return new ReplayResponse(
                                    r.getId(),
                                    r.getAudioStream().getTitle(),
                                    band != null ? band.bandName() : "",
                                    r.getViewCount()
                            );
                        })
                        .toList(),
                nextCursor, hasNext
        );
    }

    /*
     * 전체 밴드 다시보기 목록 조회 (최신순/인기순, 커서 페이징)
     * 1페이지만 캐싱: 커서 페이지까지 캐싱하면 Redis 키가 무한히 늘어남 (TTL은 CacheConfig 참고)
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REPLAY_ALL, key = "'HEAD:' + #sort + ':' + #size",
            condition = "#cursor == null")
    public CursorPage<ReplayResponse> getAllReplays(Long cursor, int size, ReplaySort sort) {

        // 인기순은 (viewCount, id) 복합 keyset이 필요해 커서 행의 viewCount를 조회
        // 커서 다시보기가 삭제된 경우 빈 페이지로 종료 (중복 페이지 방지, getUpcomingLives와 동일 정책)
        Long cursorViewCount = null;
        if (cursor != null && sort == ReplaySort.POPULAR) {
            cursorViewCount = streamReplayRepository.findById(cursor)
                    .map(StreamReplay::getViewCount)
                    .orElse(null);

            if (cursorViewCount == null)
                return CursorPage.empty();
        }

        List<StreamReplay> rows = switch (sort) {
            case LATEST -> streamReplayRepository.findReplayPageLatest(cursor, PageRequest.ofSize(size + 1));
            case POPULAR -> streamReplayRepository.findReplayPagePopular(cursorViewCount, cursor, PageRequest.ofSize(size + 1));
        };

        return assemblePage(rows, size);
    }
}
