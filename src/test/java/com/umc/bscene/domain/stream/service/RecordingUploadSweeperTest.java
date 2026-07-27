package com.umc.bscene.domain.stream.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 녹화 재업로드 스위퍼 단위 테스트.
 *
 * <p>RECORDINGS_ROOT("/recordings")는 상수이고 테스트 환경에서 쓰기가 불가능하다.
 * 다만 스위퍼는 {@code RECORDINGS_ROOT.resolve(streamPath)}로 디렉터리를 만들므로,
 * 절대 경로를 pending 경로로 주면 resolve가 그 절대 경로를 그대로 돌려준다는 점을 이용해
 * {@code @TempDir} 아래에서 디렉터리 순회 로직까지 실제로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordingUploadSweeper")
class RecordingUploadSweeperTest {

    private static final String S3_KEY_PREFIX = "recordings/";

    @Mock
    private RecordingUploadService recordingUploadService;

    @TempDir
    Path recordingDir;

    private RecordingUploadSweeper sweeper;

    @BeforeEach
    void setUp() {
        sweeper = new RecordingUploadSweeper(recordingUploadService);
        ReflectionTestUtils.setField(sweeper, "stableAfterMs", 0L);
    }

    /** 스위퍼가 "안정화됨"으로 판정하도록 수정 시각을 충분히 과거로 돌린 세그먼트 파일. */
    private Path stableSegment(String fileName) throws IOException {
        Path file = recordingDir.resolve(fileName);
        Files.write(file, new byte[16]);
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() - 3_600_000L));
        return file;
    }

    private String keyFor(Path streamDir, String fileName) {
        return S3_KEY_PREFIX + streamDir + "/" + fileName;
    }

    private String missingStreamPath() {
        return "/tmp/no-such-recording-dir-" + System.nanoTime();
    }

    @Nested
    @DisplayName("pending 경로가 없을 때")
    class NoPendingPaths {

        @Test
        @DisplayName("pending 경로가 비어 있으면 즉시 반환하고 더 이상 아무것도 하지 않는다")
        void returnsImmediatelyWhenNoPendingPaths() {
            when(recordingUploadService.getPendingStreamPaths()).thenReturn(Set.of());

            sweeper.sweep();

            verify(recordingUploadService).getPendingStreamPaths();
            verifyNoMoreInteractions(recordingUploadService);
        }
    }

    @Nested
    @DisplayName("디렉터리가 없는 pending 경로")
    class MissingDirectory {

        @Test
        @DisplayName("디렉터리가 없으면 예외 없이 건너뛴다 (continue이므로 완료 판정도 수행되지 않는다)")
        void skipsMissingDirectoryWithoutThrowing() {
            String missing = missingStreamPath();
            when(recordingUploadService.getPendingStreamPaths()).thenReturn(Set.of(missing));

            assertThatCode(sweeper::sweep).doesNotThrowAnyException();

            verify(recordingUploadService, never()).uploadResumable(any(Path.class), anyString());
            verify(recordingUploadService, never()).tryCompleteReplay(anyString());
        }

        @Test
        @DisplayName("일부 경로의 디렉터리가 없어도 나머지 경로는 계속 처리한다")
        void continuesToRemainingPathsWhenOneDirectoryIsMissing() throws IOException {
            Path file = stableSegment("segment-0.mp4");
            when(recordingUploadService.getPendingStreamPaths())
                    .thenReturn(Set.of(missingStreamPath(), recordingDir.toString()));

            sweeper.sweep();

            verify(recordingUploadService).uploadResumable(file, keyFor(recordingDir, "segment-0.mp4"));
            verify(recordingUploadService).tryCompleteReplay(recordingDir.toString());
        }
    }

    @Nested
    @DisplayName("세그먼트 재업로드")
    class ResumeUpload {

        @Test
        @DisplayName("안정화된 .mp4를 모두 재업로드하고 마지막에 완료 판정을 재시도한다")
        void uploadsStableSegmentsThenRetriesCompletion() throws IOException {
            Path first = stableSegment("a-segment.mp4");
            Path second = stableSegment("b-segment.mp4");
            when(recordingUploadService.getPendingStreamPaths()).thenReturn(Set.of(recordingDir.toString()));

            sweeper.sweep();

            verify(recordingUploadService).uploadResumable(first, keyFor(recordingDir, "a-segment.mp4"));
            verify(recordingUploadService).uploadResumable(second, keyFor(recordingDir, "b-segment.mp4"));
            verify(recordingUploadService).tryCompleteReplay(recordingDir.toString());
        }

        @Test
        @DisplayName("한 파일의 업로드 예외가 나머지 파일과 완료 판정을 중단시키지 않는다")
        void uploadExceptionDoesNotAbortSweep() throws IOException {
            Path failing = stableSegment("a-segment.mp4");
            Path healthy = stableSegment("b-segment.mp4");
            when(recordingUploadService.getPendingStreamPaths()).thenReturn(Set.of(recordingDir.toString()));
            doThrow(new RuntimeException("upload failed"))
                    .when(recordingUploadService)
                    .uploadResumable(eq(failing), eq(keyFor(recordingDir, "a-segment.mp4")));

            assertThatCode(sweeper::sweep).doesNotThrowAnyException();

            verify(recordingUploadService, times(2)).uploadResumable(any(Path.class), anyString());
            verify(recordingUploadService).uploadResumable(healthy, keyFor(recordingDir, "b-segment.mp4"));
            verify(recordingUploadService).tryCompleteReplay(recordingDir.toString());
        }

        @Test
        @DisplayName(".mp4가 아닌 파일은 업로드 대상에서 제외된다")
        void ignoresNonMp4Files() throws IOException {
            Path segment = stableSegment("segment-0.mp4");
            stableSegment("segment-0.mp4.part");
            stableSegment("readme.txt");
            when(recordingUploadService.getPendingStreamPaths()).thenReturn(Set.of(recordingDir.toString()));

            sweeper.sweep();

            verify(recordingUploadService, times(1)).uploadResumable(any(Path.class), anyString());
            verify(recordingUploadService).uploadResumable(segment, keyFor(recordingDir, "segment-0.mp4"));
        }

        @Test
        @DisplayName("아직 안정화되지 않은(방금 수정된) 파일은 mediamtx가 쓰는 중일 수 있어 건너뛴다")
        void skipsRecentlyModifiedFiles() throws IOException {
            ReflectionTestUtils.setField(sweeper, "stableAfterMs", 600_000L);
            Files.write(recordingDir.resolve("segment-0.mp4"), new byte[16]);
            when(recordingUploadService.getPendingStreamPaths()).thenReturn(Set.of(recordingDir.toString()));

            sweeper.sweep();

            verify(recordingUploadService, never()).uploadResumable(any(Path.class), anyString());
            verify(recordingUploadService).tryCompleteReplay(recordingDir.toString());
        }

        @Test
        @DisplayName("업로드할 세그먼트가 하나도 없어도 완료 판정은 재시도한다")
        void retriesCompletionEvenWithoutSegments() {
            when(recordingUploadService.getPendingStreamPaths()).thenReturn(Set.of(recordingDir.toString()));

            sweeper.sweep();

            verify(recordingUploadService, never()).uploadResumable(any(Path.class), anyString());
            verify(recordingUploadService).tryCompleteReplay(recordingDir.toString());
        }
    }
}
