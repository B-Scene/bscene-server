package com.umc.bscene.domain.stream.service;

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
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.CursorPage;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * StreamReplayServiceImpl 단위 테스트.
 * 성능 튜닝 전 동작(behaviour)과 호출 형태(call shape: 호출 횟수/인자/순서)를 함께 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StreamReplayServiceImpl")
class StreamReplayServiceImplTest {

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";

    private static final Duration MARGIN = Duration.ofMinutes(10);

    @Mock
    private StreamReplayRepository streamReplayRepository;
    @Mock
    private AudioStreamRepository audioStreamRepository;
    @Mock
    private RecordingUploadService recordingUploadService;
    @Mock
    private BandMemberPort bandMemberPort;
    @Mock
    private FollowPort followPort;
    @Mock
    private S3Presigner s3Presigner;

    private StreamReplayServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StreamReplayServiceImpl(
                streamReplayRepository,
                audioStreamRepository,
                recordingUploadService,
                bandMemberPort,
                followPort,
                s3Presigner
        );

        // @Value 주입 필드는 단위 테스트에서 리플렉션으로 채운다
        ReflectionTestUtils.setField(service, "bucket", BUCKET);
        ReflectionTestUtils.setField(service, "region", REGION);
    }

    // ---------------------------------------------------------------- helpers

    private static AudioStream closed(Long id, Long broadcasterId, Long bandId) {
        return StreamFixtures.closedStream(
                id, broadcasterId, bandId,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 11, 0),
                42
        );
    }

    /** presignGetObject가 호출 순서대로 서로 다른 URL을 돌려주도록 스텁한다. */
    private void stubPresign(String... urls) throws Exception {
        List<PresignedGetObjectRequest> presigned = new ArrayList<>();
        for (String url : urls) {
            PresignedGetObjectRequest one = mock(PresignedGetObjectRequest.class);
            when(one.url()).thenReturn(URI.create(url).toURL());
            presigned.add(one);
        }

        AtomicInteger index = new AtomicInteger();
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenAnswer(invocation -> presigned.get(index.getAndIncrement()));
    }

    /** BaseException은 응답 코드를 getBaseResponseCode()로 노출한다. 메시지가 아닌 코드로 단언한다. */
    private static void assertErrorCode(Throwable thrown, StreamErrorCode expected) {
        assertThat(thrown).isInstanceOf(StreamException.class);
        assertThat(((BaseException) thrown).getBaseResponseCode()).isEqualTo(expected);
    }

    // ============================================================ A. 업로드 요청

    @Nested
    @DisplayName("requestReplayUpload - 다시보기 업로드 요청")
    class RequestReplayUpload {

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND")
        void throwsWhenAudioStreamNotFound() {
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requestReplayUpload(10L, 1L))
                    .satisfies(e -> assertErrorCode(e, StreamErrorCode.AUDIO_STREAM_NOT_FOUND));

            verifyNoInteractions(recordingUploadService);
        }

        @Test
        @DisplayName("송출자 본인이 아니면 FORBIDDEN_REQUEST")
        void throwsWhenNotBroadcaster() {
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(closed(1L, 10L, 100L)));

            assertThatThrownBy(() -> service.requestReplayUpload(99L, 1L))
                    .satisfies(e -> assertErrorCode(e, StreamErrorCode.FORBIDDEN_REQUEST));

            verifyNoInteractions(recordingUploadService);
        }

        @Test
        @DisplayName("방송이 종료되지 않았으면 STREAM_NOT_CLOSED")
        void throwsWhenStreamNotClosed() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));

            assertThatThrownBy(() -> service.requestReplayUpload(10L, 1L))
                    .satisfies(e -> assertErrorCode(e, StreamErrorCode.STREAM_NOT_CLOSED));

            verifyNoInteractions(recordingUploadService);
        }

        @Test
        @DisplayName("녹화 세그먼트가 없으면 RECORDING_NOT_FOUND, markPending도 호출되지 않는다")
        void throwsWhenNoSegments() {
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(closed(1L, 10L, 100L)));
            when(recordingUploadService.findSegments("path-1")).thenReturn(List.of());

            assertThatThrownBy(() -> service.requestReplayUpload(10L, 1L))
                    .satisfies(e -> assertErrorCode(e, StreamErrorCode.RECORDING_NOT_FOUND));

            verify(recordingUploadService).findSegments("path-1");
            verify(recordingUploadService, never()).markPending(anyString(), anyInt());
            verify(recordingUploadService, never()).uploadAsync(anyString(), anyString());
        }

        @Test
        @DisplayName("markPending(path, 세그먼트 수)이 uploadAsync보다 먼저 정확히 한 번 호출된다")
        void marksPendingBeforeUploads() {
            Path first = Path.of("/recordings/path-1/seg-0001.mp4");
            Path second = Path.of("/recordings/path-1/seg-0002.mp4");
            Path third = Path.of("/recordings/path-1/seg-0003.mp4");

            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(closed(1L, 10L, 100L)));
            when(recordingUploadService.findSegments("path-1")).thenReturn(List.of(first, second, third));

            service.requestReplayUpload(10L, 1L);

            InOrder order = inOrder(recordingUploadService);
            order.verify(recordingUploadService).findSegments("path-1");
            order.verify(recordingUploadService).markPending("path-1", 3);
            order.verify(recordingUploadService).uploadAsync("path-1", first.toString());
            order.verify(recordingUploadService).uploadAsync("path-1", second.toString());
            order.verify(recordingUploadService).uploadAsync("path-1", third.toString());
            order.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("uploadAsync 호출 수는 세그먼트 수와 정확히 일치한다 (3개 -> 3회)")
        void uploadCountMatchesSegmentCountExactly() {
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(closed(1L, 10L, 100L)));
            when(recordingUploadService.findSegments("path-1")).thenReturn(List.of(
                    Path.of("/recordings/path-1/seg-0001.mp4"),
                    Path.of("/recordings/path-1/seg-0002.mp4"),
                    Path.of("/recordings/path-1/seg-0003.mp4")
            ));

            service.requestReplayUpload(10L, 1L);

            verify(recordingUploadService, times(1)).markPending("path-1", 3);
            verify(recordingUploadService, times(3)).uploadAsync(eq("path-1"), anyString());
            verify(recordingUploadService, times(1)).findSegments("path-1");
            verifyNoMoreInteractions(recordingUploadService);
        }

        @Test
        @DisplayName("세그먼트가 1개면 uploadAsync도 1회만 호출된다")
        void singleSegmentTriggersSingleUpload() {
            Path only = Path.of("/recordings/path-1/seg-0001.mp4");

            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(closed(1L, 10L, 100L)));
            when(recordingUploadService.findSegments("path-1")).thenReturn(List.of(only));

            service.requestReplayUpload(10L, 1L);

            verify(recordingUploadService, times(1)).markPending("path-1", 1);
            verify(recordingUploadService, times(1)).uploadAsync("path-1", only.toString());
            verify(recordingUploadService, times(1)).findSegments("path-1");
            verifyNoMoreInteractions(recordingUploadService);
        }
    }

    // ============================================================ B. 다시보기 재생

    @Nested
    @DisplayName("watchReplay - 다시보기 상세 조회")
    class WatchReplay {

        @Test
        @DisplayName("세그먼트가 없으면 REPLAY_NOT_FOUND")
        void throwsWhenNoSegments() {
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.watchReplay(1L))
                    .satisfies(e -> assertErrorCode(e, StreamErrorCode.REPLAY_NOT_FOUND));

            verify(streamReplayRepository, never()).increaseViewCount(any());
            verifyNoInteractions(bandMemberPort);
        }

        @Test
        @DisplayName("조회수는 대표(첫) 세그먼트 행에만 1회 증가시키고, 응답에는 +1된 값을 담는다")
        void increasesViewCountOnceOnFirstSegment() {
            AudioStream audioStream = closed(1L, 10L, 100L);
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of(
                    StreamFixtures.replay(11L, audioStream, "recordings/path-1/a.mp4", 10, 100L),
                    StreamFixtures.replay(12L, audioStream, "recordings/path-1/b.mp4", 20, 7L),
                    StreamFixtures.replay(13L, audioStream, "recordings/path-1/c.mp4", 30, 3L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet()))
                    .thenReturn(Map.of(100L, StreamFixtures.bandInfoOf("밴드", "https://cdn.test/band.png")));

            StreamReplayResponse response = service.watchReplay(1L);

            verify(streamReplayRepository, times(1)).increaseViewCount(11L);
            verify(streamReplayRepository, never()).increaseViewCount(12L);
            verify(streamReplayRepository, never()).increaseViewCount(13L);
            assertThat(response.viewCount()).isEqualTo(101L);
        }

        @Test
        @DisplayName("durationSec은 전체 세그먼트 합이고, playbackUrl은 플레이리스트 엔드포인트다")
        void sumsDurationAndBuildsPlaybackUrl() {
            AudioStream audioStream = closed(7L, 10L, 100L);
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(7L)).thenReturn(List.of(
                    StreamFixtures.replay(11L, audioStream, "recordings/path-7/a.mp4", 11, 0L),
                    StreamFixtures.replay(12L, audioStream, "recordings/path-7/b.mp4", 22, 0L),
                    StreamFixtures.replay(13L, audioStream, "recordings/path-7/c.mp4", 33, 0L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet()))
                    .thenReturn(Map.of(100L, StreamFixtures.bandInfoOf("밴드", "https://cdn.test/band.png")));

            StreamReplayResponse response = service.watchReplay(7L);

            assertThat(response.durationSec()).isEqualTo(66);
            assertThat(response.playbackUrl()).isEqualTo("/lives/7/replay/playlist");
            assertThat(response.title()).isEqualTo("title-7");
        }

        @Test
        @DisplayName("밴드 정보가 있으면 이름/이미지를 매핑하고, 라이브가 확정한 밴드 ID는 단일 원소 Set으로 한 번만 조회한다")
        void mapsBandInfoWithSingleLookup() {
            AudioStream audioStream = closed(1L, 10L, 100L);
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of(
                    StreamFixtures.replay(11L, audioStream, "recordings/path-1/a.mp4", 10, 5L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet()))
                    .thenReturn(Map.of(100L, StreamFixtures.bandInfoOf("비신", "https://cdn.test/bscene.png")));

            StreamReplayResponse response = service.watchReplay(1L);

            assertThat(response.bandName()).isEqualTo("비신");
            assertThat(response.bandProfileImageUrl()).isEqualTo("https://cdn.test/bscene.png");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
            verify(bandMemberPort, times(1)).getBandInfoByBandIds(captor.capture());
            assertThat(captor.getValue()).containsExactly(100L);
        }

        @Test
        @DisplayName("밴드 정보가 없으면 NPE 없이 빈 문자열로 대체한다")
        void fallsBackToEmptyStringsWhenBandInfoMissing() {
            AudioStream audioStream = closed(1L, 10L, 100L);
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of(
                    StreamFixtures.replay(11L, audioStream, "recordings/path-1/a.mp4", 10, 5L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());

            StreamReplayResponse response = service.watchReplay(1L);

            assertThat(response.bandName()).isEmpty();
            assertThat(response.bandProfileImageUrl()).isEmpty();
            assertThat(response.viewCount()).isEqualTo(6L);
        }
    }

    // ============================================================ C. HLS 플레이리스트

    @Nested
    @DisplayName("buildReplayPlaylist - HLS 매니페스트 생성")
    class BuildReplayPlaylist {

        private static final String URL_1 = "https://s3.test/seg1?sig=1";
        private static final String URL_2 = "https://s3.test/seg2?sig=2";
        private static final String URL_3 = "https://s3.test/seg3?sig=3";

        @Test
        @DisplayName("세그먼트가 없으면 REPLAY_NOT_FOUND")
        void throwsWhenNoSegments() {
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.buildReplayPlaylist(1L))
                    .satisfies(e -> assertErrorCode(e, StreamErrorCode.REPLAY_NOT_FOUND));

            verifyNoInteractions(s3Presigner);
        }

        @Test
        @DisplayName("단일 세그먼트: 헤더/EXT-X-MAP/EXTINF/ENDLIST가 정확히 생성되고 DISCONTINUITY는 없다")
        void singleSegmentPlaylistText() throws Exception {
            AudioStream audioStream = closed(1L, 10L, 100L);
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of(
                    StreamFixtures.replay(11L, audioStream, "recordings/path-1/a.mp4", 7, 0L)
            ));
            stubPresign(URL_1);

            String playlist = service.buildReplayPlaylist(1L);

            assertThat(playlist).isEqualTo("""
                    #EXTM3U
                    #EXT-X-VERSION:7
                    #EXT-X-TARGETDURATION:7
                    #EXT-X-PLAYLIST-TYPE:VOD
                    #EXT-X-MAP:URI="%s"
                    #EXTINF:7.0,
                    %s
                    #EXT-X-ENDLIST
                    """.formatted(URL_1, URL_1));
            assertThat(playlist).doesNotContain("#EXT-X-DISCONTINUITY");
            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("세그먼트 3개: DISCONTINUITY가 정확히 (n-1)개, 각각 2번째~n번째 EXT-X-MAP 앞에 위치한다")
        void multiSegmentPlaylistText() throws Exception {
            AudioStream audioStream = closed(1L, 10L, 100L);
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of(
                    StreamFixtures.replay(11L, audioStream, "recordings/path-1/a.mp4", 5, 0L),
                    StreamFixtures.replay(12L, audioStream, "recordings/path-1/b.mp4", 9, 0L),
                    StreamFixtures.replay(13L, audioStream, "recordings/path-1/c.mp4", 4, 0L)
            ));
            stubPresign(URL_1, URL_2, URL_3);

            String playlist = service.buildReplayPlaylist(1L);

            // TARGETDURATION은 최대 세그먼트 길이(9)
            assertThat(playlist).isEqualTo("""
                    #EXTM3U
                    #EXT-X-VERSION:7
                    #EXT-X-TARGETDURATION:9
                    #EXT-X-PLAYLIST-TYPE:VOD
                    #EXT-X-MAP:URI="%s"
                    #EXTINF:5.0,
                    %s
                    #EXT-X-DISCONTINUITY
                    #EXT-X-MAP:URI="%s"
                    #EXTINF:9.0,
                    %s
                    #EXT-X-DISCONTINUITY
                    #EXT-X-MAP:URI="%s"
                    #EXTINF:4.0,
                    %s
                    #EXT-X-ENDLIST
                    """.formatted(URL_1, URL_1, URL_2, URL_2, URL_3, URL_3));

            assertThat(playlist.split("#EXT-X-DISCONTINUITY", -1)).hasSize(3); // 마커 2개 = n-1
            assertThat(playlist.indexOf("#EXT-X-DISCONTINUITY"))
                    .isLessThan(playlist.indexOf("#EXT-X-MAP:URI=\"" + URL_2 + "\""));
            assertThat(playlist.lastIndexOf("#EXT-X-DISCONTINUITY"))
                    .isLessThan(playlist.indexOf("#EXT-X-MAP:URI=\"" + URL_3 + "\""));
            assertThat(playlist).startsWith("#EXTM3U\n").endsWith("#EXT-X-ENDLIST\n");
        }

        @Test
        @DisplayName("모든 세그먼트 길이가 0이면 TARGETDURATION은 하한값 1이다")
        void targetDurationFloorsAtOne() throws Exception {
            AudioStream audioStream = closed(1L, 10L, 100L);
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of(
                    StreamFixtures.replay(11L, audioStream, "recordings/path-1/a.mp4", 0, 0L),
                    StreamFixtures.replay(12L, audioStream, "recordings/path-1/b.mp4", 0, 0L)
            ));
            stubPresign(URL_1, URL_2);

            String playlist = service.buildReplayPlaylist(1L);

            assertThat(playlist).contains("#EXT-X-TARGETDURATION:1\n");
            assertThat(playlist).contains("#EXTINF:0.0,\n");
        }

        @Test
        @DisplayName("presign 만료는 10분 + 전체 재생 길이이고, 버킷/키는 세그먼트별로 전달된다")
        void presignRequestCarriesExpirationBucketAndKey() throws Exception {
            AudioStream audioStream = closed(1L, 10L, 100L);
            when(streamReplayRepository.findAllByAudioStream_IdOrderByS3KeyAsc(1L)).thenReturn(List.of(
                    StreamFixtures.replay(11L, audioStream, "recordings/path-1/a.mp4", 5, 0L),
                    StreamFixtures.replay(12L, audioStream, "recordings/path-1/b.mp4", 9, 0L),
                    StreamFixtures.replay(13L, audioStream, "recordings/path-1/c.mp4", 4, 0L)
            ));
            stubPresign(URL_1, URL_2, URL_3);

            service.buildReplayPlaylist(1L);

            ArgumentCaptor<GetObjectPresignRequest> captor =
                    ArgumentCaptor.forClass(GetObjectPresignRequest.class);
            verify(s3Presigner, times(3)).presignGetObject(captor.capture());

            Duration expected = MARGIN.plusSeconds(5 + 9 + 4);
            assertThat(captor.getAllValues())
                    .extracting(GetObjectPresignRequest::signatureDuration)
                    .containsExactly(expected, expected, expected);

            assertThat(captor.getAllValues())
                    .extracting(request -> request.getObjectRequest().bucket())
                    .containsExactly(BUCKET, BUCKET, BUCKET);

            assertThat(captor.getAllValues())
                    .extracting(request -> request.getObjectRequest().key())
                    .containsExactly(
                            "recordings/path-1/a.mp4",
                            "recordings/path-1/b.mp4",
                            "recordings/path-1/c.mp4"
                    );
        }
    }

    // ============================================================ D. 목록 조회 (assemblePage 공통)

    @Nested
    @DisplayName("getAllReplays - 전체 다시보기 목록")
    class GetAllReplays {

        @Test
        @DisplayName("LATEST는 findReplayPageLatest를 size+1 Pageable로 호출한다")
        void latestRoutesToLatestQuery() {
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(11)))
                    .thenReturn(List.of());

            service.getAllReplays(null, 10, ReplaySort.LATEST);

            verify(streamReplayRepository, times(1)).findReplayPageLatest(null, PageRequest.ofSize(11));
            verify(streamReplayRepository, never()).findReplayPagePopular(any(), any(), any());
        }

        @Test
        @DisplayName("POPULAR는 findReplayPagePopular를 size+1 Pageable로 호출한다")
        void popularRoutesToPopularQuery() {
            when(streamReplayRepository.findReplayPagePopular(null, null, PageRequest.ofSize(6)))
                    .thenReturn(List.of());

            service.getAllReplays(null, 5, ReplaySort.POPULAR);

            verify(streamReplayRepository, times(1))
                    .findReplayPagePopular(null, null, PageRequest.ofSize(6));
            verify(streamReplayRepository, never()).findReplayPageLatest(any(), any());
        }

        @Test
        @DisplayName("POPULAR + 커서: 커서 행의 viewCount를 조회해 그대로 전달한다")
        void popularWithCursorLooksUpCursorViewCount() {
            AudioStream audioStream = closed(1L, 10L, 100L);
            when(streamReplayRepository.findById(55L)).thenReturn(Optional.of(
                    StreamFixtures.replay(55L, audioStream, "recordings/path-1/a.mp4", 10, 777L)
            ));
            when(streamReplayRepository.findReplayPagePopular(777L, 55L, PageRequest.ofSize(4)))
                    .thenReturn(List.of());

            service.getAllReplays(55L, 3, ReplaySort.POPULAR);

            verify(streamReplayRepository, times(1)).findById(55L);
            verify(streamReplayRepository, times(1))
                    .findReplayPagePopular(777L, 55L, PageRequest.ofSize(4));
        }

        @Test
        @DisplayName("POPULAR + 커서 행이 사라졌으면 빈 페이지, 목록 쿼리는 아예 실행하지 않는다")
        void popularWithMissingCursorRowReturnsEmpty() {
            when(streamReplayRepository.findById(55L)).thenReturn(Optional.empty());

            CursorPage<ReplayResponse> page = service.getAllReplays(55L, 3, ReplaySort.POPULAR);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            verify(streamReplayRepository, never()).findReplayPagePopular(any(), any(), any());
            verify(streamReplayRepository, never()).findReplayPageLatest(any(), any());
            verifyNoInteractions(bandMemberPort);
        }

        @Test
        @DisplayName("LATEST + 커서는 viewCount를 조회하지 않는다 (findById 미호출)")
        void latestWithCursorSkipsViewCountLookup() {
            when(streamReplayRepository.findReplayPageLatest(55L, PageRequest.ofSize(4)))
                    .thenReturn(List.of());

            service.getAllReplays(55L, 3, ReplaySort.LATEST);

            verify(streamReplayRepository, never()).findById(any());
            verify(streamReplayRepository, times(1)).findReplayPageLatest(55L, PageRequest.ofSize(4));
        }

        @Test
        @DisplayName("빈 페이지면 밴드 조회도 재생 길이 합산 쿼리도 호출하지 않는다")
        void emptyPageSkipsBothBatchQueries() {
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(4)))
                    .thenReturn(List.of());

            CursorPage<ReplayResponse> page = service.getAllReplays(null, 3, ReplaySort.LATEST);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            verifyNoInteractions(bandMemberPort);
            verify(streamReplayRepository, never()).sumDurationSecByAudioStreamIds(any());
        }

        @Test
        @DisplayName("N+1 방지: 행이 N개여도 밴드 조회 1회, 재생 길이 합산 1회만 실행한다")
        void batchesBandAndDurationLookupsOnce() {
            List<StreamReplay> rows = List.of(
                    StreamFixtures.replay(31L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 1, 0L),
                    StreamFixtures.replay(32L, closed(2L, 12L, 200L), "recordings/path-2/a.mp4", 2, 0L),
                    StreamFixtures.replay(33L, closed(3L, 13L, 300L), "recordings/path-3/a.mp4", 3, 0L)
            );
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(11))).thenReturn(rows);
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet())).thenReturn(List.of());

            service.getAllReplays(null, 10, ReplaySort.LATEST);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Set<Long>> bandIds = ArgumentCaptor.forClass(Set.class);
            verify(bandMemberPort, times(1)).getBandInfoByBandIds(bandIds.capture());
            assertThat(bandIds.getValue()).containsExactlyInAnyOrder(100L, 200L, 300L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> audioStreamIds = ArgumentCaptor.forClass(Collection.class);
            verify(streamReplayRepository, times(1))
                    .sumDurationSecByAudioStreamIds(audioStreamIds.capture());
            assertThat(audioStreamIds.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("밴드가 중복된 행들은 Set으로 합쳐져 한 번만 조회된다")
        void duplicateBandIdsCollapseIntoOneSetEntry() {
            AudioStream first = closed(1L, 11L, 100L);
            AudioStream second = closed(2L, 11L, 100L);
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(11))).thenReturn(List.of(
                    StreamFixtures.replay(31L, first, "recordings/path-1/a.mp4", 1, 0L),
                    StreamFixtures.replay(32L, second, "recordings/path-2/a.mp4", 2, 0L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet())).thenReturn(List.of());

            service.getAllReplays(null, 10, ReplaySort.LATEST);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Set<Long>> bandIds = ArgumentCaptor.forClass(Set.class);
            verify(bandMemberPort, times(1)).getBandInfoByBandIds(bandIds.capture());
            assertThat(bandIds.getValue()).containsExactly(100L);
        }

        @Test
        @DisplayName("행 수 < size: hasNext=false, nextCursor=null")
        void rowsFewerThanSize() {
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(4))).thenReturn(List.of(
                    StreamFixtures.replay(31L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 1, 0L),
                    StreamFixtures.replay(32L, closed(2L, 12L, 200L), "recordings/path-2/a.mp4", 2, 0L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet())).thenReturn(List.of());

            CursorPage<ReplayResponse> page = service.getAllReplays(null, 3, ReplaySort.LATEST);

            assertThat(page.getItems()).hasSize(2);
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }

        @Test
        @DisplayName("행 수 == size: hasNext=false, nextCursor=null (다음 페이지 없음)")
        void rowsEqualToSize() {
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(4))).thenReturn(List.of(
                    StreamFixtures.replay(31L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 1, 0L),
                    StreamFixtures.replay(32L, closed(2L, 12L, 200L), "recordings/path-2/a.mp4", 2, 0L),
                    StreamFixtures.replay(33L, closed(3L, 13L, 300L), "recordings/path-3/a.mp4", 3, 0L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet())).thenReturn(List.of());

            CursorPage<ReplayResponse> page = service.getAllReplays(null, 3, ReplaySort.LATEST);

            assertThat(page.getItems()).hasSize(3);
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }

        @Test
        @DisplayName("행 수 == size+1: size개로 잘라내고 hasNext=true, nextCursor=마지막 노출 행 id")
        void rowsExceedSizeBySliceOfOne() {
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(4))).thenReturn(List.of(
                    StreamFixtures.replay(31L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 1, 0L),
                    StreamFixtures.replay(32L, closed(2L, 12L, 200L), "recordings/path-2/a.mp4", 2, 0L),
                    StreamFixtures.replay(33L, closed(3L, 13L, 300L), "recordings/path-3/a.mp4", 3, 0L),
                    StreamFixtures.replay(34L, closed(4L, 14L, 400L), "recordings/path-4/a.mp4", 4, 0L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet())).thenReturn(List.of());

            CursorPage<ReplayResponse> page = service.getAllReplays(null, 3, ReplaySort.LATEST);

            assertThat(page.getItems()).extracting(ReplayResponse::liveId)
                    .containsExactly(1L, 2L, 3L);
            assertThat(page.getPageInfo().hasNext()).isTrue();
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(33L);

            // 잘려나간 4번째 행은 배치 조회 대상에서도 제외된다
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> audioStreamIds = ArgumentCaptor.forClass(Collection.class);
            verify(streamReplayRepository).sumDurationSecByAudioStreamIds(audioStreamIds.capture());
            assertThat(audioStreamIds.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("durationSec: 합산 맵에 있으면 합산값, 없으면 해당 행의 durationSec으로 대체한다")
        void durationComesFromSumMapWithRowFallback() {
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(11))).thenReturn(List.of(
                    StreamFixtures.replay(31L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 30, 0L),
                    StreamFixtures.replay(32L, closed(2L, 12L, 200L), "recordings/path-2/a.mp4", 40, 0L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            // 라이브 1만 합산 결과가 있고, 라이브 2는 누락
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet()))
                    .thenReturn(List.of(StreamFixtures.durationSum(1L, 123L)));

            CursorPage<ReplayResponse> page = service.getAllReplays(null, 10, ReplaySort.LATEST);

            assertThat(page.getItems()).extracting(ReplayResponse::durationSec)
                    .containsExactly(123, 40);
        }

        @Test
        @DisplayName("밴드 정보가 있으면 이름을 매핑하고, 없는 행은 빈 문자열로 대체한다")
        void mapsBandNameWithEmptyFallback() {
            when(streamReplayRepository.findReplayPageLatest(null, PageRequest.ofSize(11))).thenReturn(List.of(
                    StreamFixtures.replay(31L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 30, 5L),
                    StreamFixtures.replay(32L, closed(2L, 12L, 200L), "recordings/path-2/a.mp4", 40, 6L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet()))
                    .thenReturn(Map.of(100L, StreamFixtures.bandInfoOf("비신", "https://cdn.test/b.png")));
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet())).thenReturn(List.of());

            CursorPage<ReplayResponse> page = service.getAllReplays(null, 10, ReplaySort.LATEST);

            assertThat(page.getItems()).extracting(ReplayResponse::bandName)
                    .containsExactly("비신", "");
            assertThat(page.getItems()).extracting(ReplayResponse::title)
                    .containsExactly("title-1", "title-2");
            assertThat(page.getItems()).extracting(ReplayResponse::viewCount)
                    .containsExactly(5L, 6L);
        }
    }

    @Nested
    @DisplayName("getFollowingReplays - 팔로우 밴드 다시보기 목록")
    class GetFollowingReplays {

        @Test
        @DisplayName("LATEST는 findReplayPageLatestByBandIds를 size+1 Pageable로 호출한다")
        void latestRoutesToBandScopedLatestQuery() {
            when(followPort.getFollowingBandIds(10L)).thenReturn(List.of(100L, 200L));
            when(streamReplayRepository.findReplayPageLatestByBandIds(
                    List.of(100L, 200L), null, PageRequest.ofSize(11))).thenReturn(List.of());

            service.getFollowingReplays(10L, null, 10, ReplaySort.LATEST);

            verify(streamReplayRepository, times(1))
                    .findReplayPageLatestByBandIds(List.of(100L, 200L), null, PageRequest.ofSize(11));
            verify(streamReplayRepository, never())
                    .findReplayPagePopularByBandIds(anyList(), any(), any(), any());
        }

        @Test
        @DisplayName("POPULAR는 findReplayPagePopularByBandIds를 size+1 Pageable로 호출한다")
        void popularRoutesToBandScopedPopularQuery() {
            when(followPort.getFollowingBandIds(10L)).thenReturn(List.of(100L));
            when(streamReplayRepository.findReplayPagePopularByBandIds(
                    List.of(100L), null, null, PageRequest.ofSize(6))).thenReturn(List.of());

            service.getFollowingReplays(10L, null, 5, ReplaySort.POPULAR);

            verify(streamReplayRepository, times(1))
                    .findReplayPagePopularByBandIds(List.of(100L), null, null, PageRequest.ofSize(6));
            verify(streamReplayRepository, never())
                    .findReplayPageLatestByBandIds(anyList(), any(), any());
        }

        @Test
        @DisplayName("POPULAR + 커서: 커서 행의 viewCount를 조회해 그대로 전달한다")
        void popularWithCursorLooksUpCursorViewCount() {
            when(streamReplayRepository.findById(55L)).thenReturn(Optional.of(
                    StreamFixtures.replay(55L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 10, 88L)
            ));
            when(followPort.getFollowingBandIds(10L)).thenReturn(List.of(100L));
            when(streamReplayRepository.findReplayPagePopularByBandIds(
                    List.of(100L), 88L, 55L, PageRequest.ofSize(4))).thenReturn(List.of());

            service.getFollowingReplays(10L, 55L, 3, ReplaySort.POPULAR);

            verify(streamReplayRepository, times(1)).findById(55L);
            verify(streamReplayRepository, times(1))
                    .findReplayPagePopularByBandIds(List.of(100L), 88L, 55L, PageRequest.ofSize(4));
        }

        @Test
        @DisplayName("POPULAR + 커서 행이 사라졌으면 빈 페이지, 팔로우 조회도 목록 쿼리도 실행하지 않는다")
        void popularWithMissingCursorRowReturnsEmpty() {
            when(streamReplayRepository.findById(55L)).thenReturn(Optional.empty());

            CursorPage<ReplayResponse> page = service.getFollowingReplays(10L, 55L, 3, ReplaySort.POPULAR);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            verifyNoInteractions(followPort);
            verify(streamReplayRepository, never())
                    .findReplayPagePopularByBandIds(anyList(), any(), any(), any());
        }

        @Test
        @DisplayName("LATEST + 커서는 viewCount를 조회하지 않는다 (findById 미호출)")
        void latestWithCursorSkipsViewCountLookup() {
            when(followPort.getFollowingBandIds(10L)).thenReturn(List.of(100L));
            when(streamReplayRepository.findReplayPageLatestByBandIds(
                    List.of(100L), 55L, PageRequest.ofSize(4))).thenReturn(List.of());

            service.getFollowingReplays(10L, 55L, 3, ReplaySort.LATEST);

            verify(streamReplayRepository, never()).findById(any());
        }

        @Test
        @DisplayName("팔로우한 밴드가 없으면 빈 페이지, 목록 쿼리는 아예 실행하지 않는다")
        void emptyFollowedBandsReturnsEmptyPage() {
            when(followPort.getFollowingBandIds(10L)).thenReturn(List.of());

            CursorPage<ReplayResponse> page = service.getFollowingReplays(10L, null, 3, ReplaySort.LATEST);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            verify(streamReplayRepository, never()).findReplayPageLatestByBandIds(anyList(), any(), any());
            verify(streamReplayRepository, never())
                    .findReplayPagePopularByBandIds(anyList(), any(), any(), any());
            verifyNoInteractions(bandMemberPort);
        }

        @Test
        @DisplayName("N+1 방지: 행이 N개여도 밴드 조회 1회, 재생 길이 합산 1회만 실행한다")
        void batchesBandAndDurationLookupsOnce() {
            when(followPort.getFollowingBandIds(10L)).thenReturn(List.of(100L, 200L, 300L));
            when(streamReplayRepository.findReplayPageLatestByBandIds(
                    List.of(100L, 200L, 300L), null, PageRequest.ofSize(11))).thenReturn(List.of(
                    StreamFixtures.replay(31L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 1, 0L),
                    StreamFixtures.replay(32L, closed(2L, 12L, 200L), "recordings/path-2/a.mp4", 2, 0L),
                    StreamFixtures.replay(33L, closed(3L, 13L, 300L), "recordings/path-3/a.mp4", 3, 0L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet())).thenReturn(List.of());

            service.getFollowingReplays(10L, null, 10, ReplaySort.LATEST);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Set<Long>> bandIds = ArgumentCaptor.forClass(Set.class);
            verify(bandMemberPort, times(1)).getBandInfoByBandIds(bandIds.capture());
            assertThat(bandIds.getValue()).containsExactlyInAnyOrder(100L, 200L, 300L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> audioStreamIds = ArgumentCaptor.forClass(Collection.class);
            verify(streamReplayRepository, times(1))
                    .sumDurationSecByAudioStreamIds(audioStreamIds.capture());
            assertThat(audioStreamIds.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("행 수 == size+1: size개로 잘라내고 hasNext=true, nextCursor=마지막 노출 행 id")
        void slicesAtSizePlusOneBoundary() {
            when(followPort.getFollowingBandIds(10L)).thenReturn(List.of(100L));
            when(streamReplayRepository.findReplayPageLatestByBandIds(
                    List.of(100L), null, PageRequest.ofSize(3))).thenReturn(List.of(
                    StreamFixtures.replay(31L, closed(1L, 11L, 100L), "recordings/path-1/a.mp4", 1, 0L),
                    StreamFixtures.replay(32L, closed(2L, 12L, 100L), "recordings/path-2/a.mp4", 2, 0L),
                    StreamFixtures.replay(33L, closed(3L, 13L, 100L), "recordings/path-3/a.mp4", 3, 0L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet()))
                    .thenReturn(List.of(StreamFixtures.durationSum(1L, 11L), StreamFixtures.durationSum(2L, 22L)));

            CursorPage<ReplayResponse> page = service.getFollowingReplays(10L, null, 2, ReplaySort.LATEST);

            assertThat(page.getItems()).extracting(ReplayResponse::liveId).containsExactly(1L, 2L);
            assertThat(page.getItems()).extracting(ReplayResponse::durationSec).containsExactly(11, 22);
            assertThat(page.getPageInfo().hasNext()).isTrue();
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(32L);
        }

        @Test
        @DisplayName("응답 항목에 원본 라이브의 썸네일이 그대로 실린다")
        void carriesAudioStreamThumbnail() {
            AudioStream audioStream = StreamFixtures.stream(1L, 11L, 100L, StreamStatus.CLOSED);
            when(followPort.getFollowingBandIds(10L)).thenReturn(List.of(100L));
            when(streamReplayRepository.findReplayPageLatestByBandIds(
                    List.of(100L), null, PageRequest.ofSize(11))).thenReturn(List.of(
                    StreamFixtures.replay(31L, audioStream, "recordings/path-1/a.mp4", 9, 3L)
            ));
            when(bandMemberPort.getBandInfoByBandIds(anySet())).thenReturn(Map.of());
            when(streamReplayRepository.sumDurationSecByAudioStreamIds(anySet())).thenReturn(List.of());

            CursorPage<ReplayResponse> page = service.getFollowingReplays(10L, null, 10, ReplaySort.LATEST);

            assertThat(page.getItems()).singleElement().satisfies(item -> {
                assertThat(item.thumbnailImageUrl()).isEqualTo("https://cdn.test/thumb-1.jpg");
                assertThat(item.durationSec()).isEqualTo(9);   // 합산 결과 없음 -> 행 값 대체
                assertThat(item.bandName()).isEmpty();
            });
        }
    }
}
