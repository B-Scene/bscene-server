package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 녹화 세그먼트 S3 멀티파트 업로드 서비스 단위 테스트.
 *
 * <p>RECORDINGS_ROOT("/recordings")는 상수로 하드코딩되어 있어 실제 파일을 만들 수 없다.
 * 따라서 uploadAsync는 파일 시스템에 닿기 전에 걸러지는 경로 검증만 문자열 경로로 검증하고,
 * 실제 업로드 흐름은 /recordings 접두사를 검사하지 않는 uploadResumable(패키지 가시성)에
 * {@code @TempDir} 임시 파일을 직접 넘겨 종단 간으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordingUploadService")
class RecordingUploadServiceTest {

    private static final String BUCKET = "test-bucket";

    private static final String STREAM_PATH = "path-1";
    private static final String S3_KEY = "recordings/path-1/segment-0.mp4";
    private static final String LOCK_KEY = "recording:lock:" + S3_KEY;
    private static final String UPLOAD_ID_KEY = "recording:upload:" + S3_KEY;
    private static final String PENDING_KEY = "recording:pending:" + STREAM_PATH;
    private static final String EXPECTED_KEY = "recording:expected:" + STREAM_PATH;

    private static final Duration LOCK_TTL = Duration.ofHours(1);
    private static final Duration KEY_TTL = Duration.ofHours(73);

    private static final long PART_SIZE = 8L * 1024 * 1024;

    @Mock
    private S3Client s3Client;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private StreamReplayRepository streamReplayRepository;

    @Mock
    private AudioStreamRepository audioStreamRepository;

    @Mock
    private ReplayNotificationService replayNotificationService;

    @TempDir
    Path tempDir;

    private RecordingUploadService recordingUploadService;

    @BeforeEach
    void setUp() {
        recordingUploadService = new RecordingUploadService(
                s3Client, redisTemplate, streamReplayRepository, audioStreamRepository, replayNotificationService
        );
        ReflectionTestUtils.setField(recordingUploadService, "bucket", BUCKET);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private Path segmentFile(String name, int size) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, new byte[size]);
        return file;
    }

    private void givenLockAcquired() {
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class))).thenReturn(true);
    }

    private void givenNewMultipartSession(String uploadId) {
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId(uploadId).build());
    }

    private void givenUploadPartReturns(String eTag) {
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenReturn(UploadPartResponse.builder().eTag(eTag).build());
    }

    @Nested
    @DisplayName("uploadAsync 경로 검증")
    class UploadAsyncGuard {

        @Test
        @DisplayName("/recordings 외부 경로는 거부되고 S3를 건드리지 않는다")
        void rejectsPathOutsideRecordingsRoot() {
            recordingUploadService.uploadAsync(STREAM_PATH, "/etc/passwd");

            verifyNoInteractions(s3Client, valueOperations, streamReplayRepository,
                    audioStreamRepository, replayNotificationService);
        }

        @Test
        @DisplayName("상위 경로 탈출(..)은 normalize 후 /recordings 외부가 되어 거부된다")
        void rejectsPathTraversalEscapingRecordingsRoot() {
            recordingUploadService.uploadAsync(STREAM_PATH, "/recordings/../etc/x.mp4");

            verifyNoInteractions(s3Client, valueOperations, streamReplayRepository,
                    audioStreamRepository, replayNotificationService);
        }

        @Test
        @DisplayName("부모 디렉터리명이 path와 다르면 엉뚱한 AudioStream에 붙는 것을 막기 위해 거부된다")
        void rejectsWhenParentDirectoryNameDiffersFromPath() {
            recordingUploadService.uploadAsync(STREAM_PATH, "/recordings/other-path/seg.mp4");

            verifyNoInteractions(s3Client, valueOperations, streamReplayRepository,
                    audioStreamRepository, replayNotificationService);
        }

        @Test
        @DisplayName("파일이 존재하지 않으면 거부된다")
        void rejectsWhenFileDoesNotExist() {
            recordingUploadService.uploadAsync(STREAM_PATH, "/recordings/path-1/missing-segment.mp4");

            verifyNoInteractions(s3Client, valueOperations, streamReplayRepository,
                    audioStreamRepository, replayNotificationService);
        }

        @Test
        @DisplayName("경로 검증 실패는 예외를 던지지 않고 조용히 반환한다")
        void guardFailureDoesNotThrow() {
            assertThatCode(() -> recordingUploadService.uploadAsync(STREAM_PATH, "/etc/passwd"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("uploadResumable 파일 단위 Redis 락")
    class FileLock {

        @Test
        @DisplayName("다른 워커가 락을 잡고 있으면(false) 즉시 종료하고 락을 지우지 않는다")
        void skipsWhenLockHeldByAnotherWorker() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

            recordingUploadService.uploadResumable(file, S3_KEY);

            verifyNoInteractions(s3Client);
            verify(redisTemplate, never()).delete(anyString());
            verify(redisTemplate, never()).delete(anyCollection());
            assertThat(Files.exists(file)).isTrue();
        }

        @Test
        @DisplayName("setIfAbsent가 null이어도 락 미획득으로 보고 즉시 종료한다")
        void skipsWhenSetIfAbsentReturnsNull() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(null);

            recordingUploadService.uploadResumable(file, S3_KEY);

            verifyNoInteractions(s3Client);
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("락 키는 recording:lock:{s3Key}, TTL은 1시간이다")
        void usesLockKeyAndOneHourTtl() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

            recordingUploadService.uploadResumable(file, S3_KEY);

            ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
            verify(valueOperations).setIfAbsent(key.capture(), eq("1"), ttl.capture());
            assertThat(key.getValue()).isEqualTo(LOCK_KEY);
            assertThat(ttl.getValue()).isEqualTo(LOCK_TTL);
        }

        @Test
        @DisplayName("업로드 성공 시 finally에서 락이 해제된다")
        void releasesLockOnSuccess() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            givenNewMultipartSession("upload-1");
            givenUploadPartReturns("etag-1");
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(true);

            recordingUploadService.uploadResumable(file, S3_KEY);

            verify(redisTemplate).delete(LOCK_KEY);
        }

        @Test
        @DisplayName("S3 예외가 나도 finally에서 락이 해제된다")
        void releasesLockWhenS3Throws() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                    .thenThrow(S3Exception.builder().message("boom").build());

            assertThatCode(() -> recordingUploadService.uploadResumable(file, S3_KEY))
                    .doesNotThrowAnyException();

            verify(redisTemplate).delete(LOCK_KEY);
        }
    }

    @Nested
    @DisplayName("uploadResumable 멀티파트 업로드")
    class MultipartUpload {

        @Test
        @DisplayName("저장된 uploadId가 없으면 새 세션을 만들고 uploadId를 73시간 TTL로 저장한다")
        void createsNewMultipartSessionWhenNoStoredUploadId() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            when(valueOperations.get(UPLOAD_ID_KEY)).thenReturn(null);
            givenNewMultipartSession("upload-1");
            givenUploadPartReturns("etag-1");
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(true);

            recordingUploadService.uploadResumable(file, S3_KEY);

            ArgumentCaptor<CreateMultipartUploadRequest> create =
                    ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
            verify(s3Client).createMultipartUpload(create.capture());
            assertThat(create.getValue().bucket()).isEqualTo(BUCKET);
            assertThat(create.getValue().key()).isEqualTo(S3_KEY);
            assertThat(create.getValue().contentType()).isEqualTo("audio/mp4");

            verify(valueOperations).set(UPLOAD_ID_KEY, "upload-1", KEY_TTL);
            verify(s3Client).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
            verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
            verify(s3Client, never()).listParts(any(ListPartsRequest.class));
        }

        @Test
        @DisplayName("이미 올라간 파트는 재업로드하지 않고 기존 ETag를 재사용한다 (이어올리기)")
        void resumesUploadingOnlyMissingParts() throws IOException {
            // 9MB → 8MB 파트 1개 + 1MB 파트 1개 = 총 2파트. 그중 1번 파트는 이미 업로드되어 있음
            Path file = segmentFile("segment-0.mp4", (int) PART_SIZE + 1024 * 1024);
            givenLockAcquired();
            when(valueOperations.get(UPLOAD_ID_KEY)).thenReturn("upload-existing");
            when(s3Client.listParts(any(ListPartsRequest.class))).thenReturn(
                    ListPartsResponse.builder()
                            .parts(Part.builder().partNumber(1).eTag("etag-1").build())
                            .build()
            );
            givenUploadPartReturns("etag-2");
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(true);

            recordingUploadService.uploadResumable(file, S3_KEY);

            // 새 세션을 만들지 않고 기존 uploadId를 그대로 사용
            verify(s3Client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
            verify(valueOperations, never()).set(eq(UPLOAD_ID_KEY), anyString(), any(Duration.class));

            // 누락된 2번 파트만 업로드
            ArgumentCaptor<UploadPartRequest> uploaded = ArgumentCaptor.forClass(UploadPartRequest.class);
            verify(s3Client, times(1)).uploadPart(uploaded.capture(), any(RequestBody.class));
            assertThat(uploaded.getValue().partNumber()).isEqualTo(2);
            assertThat(uploaded.getValue().uploadId()).isEqualTo("upload-existing");

            // 완료 요청에는 기존 파트 ETag까지 파트 번호 순으로 포함
            ArgumentCaptor<CompleteMultipartUploadRequest> complete =
                    ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
            verify(s3Client).completeMultipartUpload(complete.capture());
            assertThat(complete.getValue().uploadId()).isEqualTo("upload-existing");
            List<CompletedPart> parts = complete.getValue().multipartUpload().parts();
            assertThat(parts).extracting(CompletedPart::partNumber).containsExactly(1, 2);
            assertThat(parts).extracting(CompletedPart::eTag).containsExactly("etag-1", "etag-2");
        }

        @Test
        @DisplayName("모든 파트가 이미 올라가 있으면 uploadPart 없이 완료만 호출한다")
        void completesWithoutUploadingWhenAllPartsPresent() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            when(valueOperations.get(UPLOAD_ID_KEY)).thenReturn("upload-existing");
            when(s3Client.listParts(any(ListPartsRequest.class))).thenReturn(
                    ListPartsResponse.builder()
                            .parts(Part.builder().partNumber(1).eTag("etag-1").build())
                            .build()
            );
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(true);

            recordingUploadService.uploadResumable(file, S3_KEY);

            verify(s3Client, never()).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
            verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        }

        @Test
        @DisplayName("저장된 uploadId가 만료(NoSuchUpload)되면 새 멀티파트 세션으로 폴백한다")
        void fallsBackToNewSessionWhenStoredUploadIdIsInvalid() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            when(valueOperations.get(UPLOAD_ID_KEY)).thenReturn("expired-upload");
            when(s3Client.listParts(any(ListPartsRequest.class)))
                    .thenThrow(NoSuchUploadException.builder().message("no such upload").build());
            givenNewMultipartSession("upload-new");
            givenUploadPartReturns("etag-1");
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(true);

            recordingUploadService.uploadResumable(file, S3_KEY);

            verify(s3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));
            verify(valueOperations).set(UPLOAD_ID_KEY, "upload-new", KEY_TTL);

            ArgumentCaptor<UploadPartRequest> uploaded = ArgumentCaptor.forClass(UploadPartRequest.class);
            verify(s3Client).uploadPart(uploaded.capture(), any(RequestBody.class));
            assertThat(uploaded.getValue().uploadId()).isEqualTo("upload-new");
            verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        }

        @Test
        @DisplayName("0바이트 세그먼트는 로컬 파일만 삭제하고 완료 요청·메타데이터 저장을 하지 않는다")
        void skipsZeroByteSegment() throws IOException {
            Path file = segmentFile("segment-0.mp4", 0);
            givenLockAcquired();
            givenNewMultipartSession("upload-1");

            recordingUploadService.uploadResumable(file, S3_KEY);

            assertThat(Files.exists(file)).isFalse();
            verify(s3Client, never()).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
            verify(s3Client, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
            verify(streamReplayRepository, never()).saveAndFlush(any(StreamReplay.class));
            verify(redisTemplate).delete(LOCK_KEY);
        }
    }

    @Nested
    @DisplayName("업로드 성공 후처리")
    class AfterSuccess {

        @Test
        @DisplayName("메타데이터 저장 → uploadId 키 삭제 → 로컬 파일 삭제 순서로 정리한다")
        void persistsReplayThenCleansUpInOrder() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CLOSED);
            givenLockAcquired();
            givenNewMultipartSession("upload-1");
            givenUploadPartReturns("etag-1");
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(false);
            when(audioStreamRepository.findByPath(STREAM_PATH)).thenReturn(Optional.of(audioStream));

            recordingUploadService.uploadResumable(file, S3_KEY);

            ArgumentCaptor<StreamReplay> saved = ArgumentCaptor.forClass(StreamReplay.class);
            InOrder order = inOrder(s3Client, streamReplayRepository, redisTemplate);
            order.verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
            order.verify(streamReplayRepository).saveAndFlush(saved.capture());
            order.verify(redisTemplate).delete(UPLOAD_ID_KEY);
            order.verify(redisTemplate).delete(LOCK_KEY);

            assertThat(saved.getValue().getS3Key()).isEqualTo(S3_KEY);
            assertThat(saved.getValue().getAudioStream()).isSameAs(audioStream);
            assertThat(saved.getValue().getViewCount()).isZero();
            assertThat(Files.exists(file)).isFalse();
        }

        @Test
        @DisplayName("이미 저장된 s3Key면 저장을 건너뛰되(멱등 재실행) 정리는 그대로 수행한다")
        void skipsSaveWhenS3KeyAlreadyPersisted() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            givenNewMultipartSession("upload-1");
            givenUploadPartReturns("etag-1");
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(true);

            recordingUploadService.uploadResumable(file, S3_KEY);

            verify(streamReplayRepository, never()).saveAndFlush(any(StreamReplay.class));
            verifyNoInteractions(audioStreamRepository);
            verify(redisTemplate).delete(UPLOAD_ID_KEY);
            assertThat(Files.exists(file)).isFalse();
        }

        @Test
        @DisplayName("path에 해당하는 AudioStream이 없으면 저장을 건너뛰고 정리는 계속 진행한다")
        void skipsSaveWhenAudioStreamMissing() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            givenNewMultipartSession("upload-1");
            givenUploadPartReturns("etag-1");
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(false);
            when(audioStreamRepository.findByPath(STREAM_PATH)).thenReturn(Optional.empty());

            assertThatCode(() -> recordingUploadService.uploadResumable(file, S3_KEY))
                    .doesNotThrowAnyException();

            verify(streamReplayRepository, never()).saveAndFlush(any(StreamReplay.class));
            verify(redisTemplate).delete(UPLOAD_ID_KEY);
            assertThat(Files.exists(file)).isFalse();
        }

        @Test
        @DisplayName("동시 저장으로 인한 DataIntegrityViolationException은 삼켜지고 업로드는 성공 처리된다")
        void swallowsDataIntegrityViolationOnConcurrentSave() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            givenNewMultipartSession("upload-1");
            givenUploadPartReturns("etag-1");
            when(streamReplayRepository.existsByS3Key(S3_KEY)).thenReturn(false);
            when(audioStreamRepository.findByPath(STREAM_PATH))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CLOSED)));
            when(streamReplayRepository.saveAndFlush(any(StreamReplay.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate s3Key"));

            assertThatCode(() -> recordingUploadService.uploadResumable(file, S3_KEY))
                    .doesNotThrowAnyException();

            verify(redisTemplate).delete(UPLOAD_ID_KEY);
            verify(redisTemplate).delete(LOCK_KEY);
            assertThat(Files.exists(file)).isFalse();
        }

        @Test
        @DisplayName("업로드 도중 S3 예외가 나면 로컬 파일과 uploadId 키를 남겨 스위퍼가 이어받게 한다")
        void keepsFileAndUploadIdKeyWhenUploadFails() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            givenNewMultipartSession("upload-1");
            when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                    .thenThrow(S3Exception.builder().message("network down").build());

            assertThatCode(() -> recordingUploadService.uploadResumable(file, S3_KEY))
                    .doesNotThrowAnyException();

            assertThat(Files.exists(file)).isTrue();
            verify(redisTemplate, never()).delete(UPLOAD_ID_KEY);
            verify(s3Client, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
            verify(streamReplayRepository, never()).saveAndFlush(any(StreamReplay.class));
            verify(redisTemplate).delete(LOCK_KEY);
        }

        @Test
        @DisplayName("완료 요청 자체가 실패해도 예외를 전파하지 않고 파일·uploadId 키를 보존한다")
        void keepsStateWhenCompleteMultipartUploadFails() throws IOException {
            Path file = segmentFile("segment-0.mp4", 1024);
            givenLockAcquired();
            givenNewMultipartSession("upload-1");
            givenUploadPartReturns("etag-1");
            when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                    .thenThrow(S3Exception.builder().message("complete failed").build());

            assertThatCode(() -> recordingUploadService.uploadResumable(file, S3_KEY))
                    .doesNotThrowAnyException();

            assertThat(Files.exists(file)).isTrue();
            verify(redisTemplate, never()).delete(UPLOAD_ID_KEY);
            verifyNoInteractions(streamReplayRepository);
            verify(redisTemplate).delete(LOCK_KEY);
        }
    }

    @Nested
    @DisplayName("tryCompleteReplay 다시보기 등록 완료 판정")
    class TryCompleteReplay {

        @Test
        @DisplayName("expected 키가 없으면 아무것도 하지 않는다")
        void noOpWhenExpectedCountKeyAbsent() {
            when(valueOperations.get(EXPECTED_KEY)).thenReturn(null);

            recordingUploadService.tryCompleteReplay(STREAM_PATH);

            verifyNoInteractions(audioStreamRepository, streamReplayRepository, replayNotificationService);
            verify(redisTemplate, never()).delete(anyString());
            verify(redisTemplate, never()).delete(anyCollection());
        }

        @Test
        @DisplayName("AudioStream을 찾지 못하면 알림 없이 종료한다")
        void noOpWhenAudioStreamMissing() {
            when(valueOperations.get(EXPECTED_KEY)).thenReturn("2");
            when(audioStreamRepository.findByPath(STREAM_PATH)).thenReturn(Optional.empty());

            recordingUploadService.tryCompleteReplay(STREAM_PATH);

            verifyNoInteractions(streamReplayRepository, replayNotificationService);
            verify(redisTemplate, never()).delete(anyCollection());
        }

        @Test
        @DisplayName("저장된 세그먼트 수가 기대치에 못 미치면 알림 없이 pending 키를 유지한다")
        void keepsPendingWhenCountNotReached() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CLOSED);
            when(valueOperations.get(EXPECTED_KEY)).thenReturn("3");
            when(audioStreamRepository.findByPath(STREAM_PATH)).thenReturn(Optional.of(audioStream));
            when(streamReplayRepository.countByAudioStream_Id(1L)).thenReturn(2L);

            recordingUploadService.tryCompleteReplay(STREAM_PATH);

            verifyNoInteractions(replayNotificationService);
            verify(redisTemplate, never()).delete(anyCollection());
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("기대치에 도달하면 알림을 발송하고 pending·expected 키를 모두 삭제한다")
        void notifiesAndDeletesKeysWhenCountReached() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CLOSED);
            when(valueOperations.get(EXPECTED_KEY)).thenReturn("2");
            when(audioStreamRepository.findByPath(STREAM_PATH)).thenReturn(Optional.of(audioStream));
            when(streamReplayRepository.countByAudioStream_Id(1L)).thenReturn(2L);

            recordingUploadService.tryCompleteReplay(STREAM_PATH);

            verify(replayNotificationService).notifyReplayReady(audioStream);
            verify(redisTemplate).delete(List.of(PENDING_KEY, EXPECTED_KEY));
        }

        @Test
        @DisplayName("expected 값이 숫자가 아니면 NumberFormatException을 삼키고 expected 키만 삭제한다")
        void swallowsNumberFormatExceptionAndDeletesExpectedKeyOnly() {
            when(valueOperations.get(EXPECTED_KEY)).thenReturn("not-a-number");

            assertThatCode(() -> recordingUploadService.tryCompleteReplay(STREAM_PATH))
                    .doesNotThrowAnyException();

            verify(redisTemplate).delete(EXPECTED_KEY);
            verify(redisTemplate, never()).delete(anyCollection());
            verifyNoInteractions(audioStreamRepository, streamReplayRepository, replayNotificationService);
        }

        @Test
        @DisplayName("알림 발송이 실패하면 예외를 삼키고 pending 키를 유지해 스위퍼가 재시도하게 한다")
        void keepsPendingWhenNotificationThrows() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CLOSED);
            when(valueOperations.get(EXPECTED_KEY)).thenReturn("1");
            when(audioStreamRepository.findByPath(STREAM_PATH)).thenReturn(Optional.of(audioStream));
            when(streamReplayRepository.countByAudioStream_Id(1L)).thenReturn(1L);
            doThrow(new RuntimeException("notify failed"))
                    .when(replayNotificationService).notifyReplayReady(audioStream);

            assertThatCode(() -> recordingUploadService.tryCompleteReplay(STREAM_PATH))
                    .doesNotThrowAnyException();

            verify(redisTemplate, never()).delete(anyCollection());
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("기대치를 초과해 저장되어 있어도 완료로 판정한다")
        void notifiesWhenCountExceedsExpected() {
            AudioStream audioStream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CLOSED);
            when(valueOperations.get(EXPECTED_KEY)).thenReturn("2");
            when(audioStreamRepository.findByPath(STREAM_PATH)).thenReturn(Optional.of(audioStream));
            when(streamReplayRepository.countByAudioStream_Id(anyLong())).thenReturn(5L);

            recordingUploadService.tryCompleteReplay(STREAM_PATH);

            verify(replayNotificationService).notifyReplayReady(audioStream);
            verify(redisTemplate).delete(List.of(PENDING_KEY, EXPECTED_KEY));
        }
    }

    @Nested
    @DisplayName("pending 키 관리")
    class PendingKeys {

        @Test
        @DisplayName("markPending은 pending·expected 키를 73시간 TTL로 기록한다")
        void marksPendingWithExpectedCount() {
            recordingUploadService.markPending(STREAM_PATH, 5);

            verify(valueOperations).set(PENDING_KEY, "1", KEY_TTL);
            verify(valueOperations).set(EXPECTED_KEY, "5", KEY_TTL);
        }

        @Test
        @DisplayName("expected 세그먼트 수가 0이어도 문자열로 그대로 기록한다")
        void marksPendingWithZeroExpectedCount() {
            recordingUploadService.markPending(STREAM_PATH, 0);

            verify(valueOperations).set(EXPECTED_KEY, "0", KEY_TTL);
        }

        @Test
        @DisplayName("getPendingStreamPaths는 recording:pending: 접두사를 제거한 경로를 반환한다")
        void stripsPendingKeyPrefix() {
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(
                    StreamFixtures.redisCursor("recording:pending:path-1", "recording:pending:path-2")
            );

            Set<String> paths = recordingUploadService.getPendingStreamPaths();

            assertThat(paths).containsExactlyInAnyOrder("path-1", "path-2");
        }

        @Test
        @DisplayName("중복 키가 스캔되어도 경로 집합은 중복 없이 반환된다")
        void deduplicatesScannedKeys() {
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(
                    StreamFixtures.redisCursor("recording:pending:path-1", "recording:pending:path-1")
            );

            assertThat(recordingUploadService.getPendingStreamPaths()).containsExactly("path-1");
        }
    }

    @Nested
    @DisplayName("findSegments")
    class FindSegments {

        @Test
        @DisplayName("/recordings 하위 디렉터리가 없으면 IOException을 삼키고 빈 리스트를 반환한다")
        void returnsEmptyListWhenDirectoryMissing() {
            List<Path> segments = recordingUploadService.findSegments("no-such-stream-path-" + System.nanoTime());

            assertThat(segments).isEmpty();
            verifyNoInteractions(s3Client, redisTemplate, streamReplayRepository);
        }
    }
}
