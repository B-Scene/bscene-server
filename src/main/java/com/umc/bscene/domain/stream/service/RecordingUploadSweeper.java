package com.umc.bscene.domain.stream.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

/*
 * 훅이 아예 실행되지 않았거나(mediamtx 크래시/OOM), 훅은 왔지만 실패했거나(spring 다운),
 * 업로드가 중간에 끊긴 녹화본을 주기적으로 다시 훑어 S3로 올려 최종적으로 정합성을 보장하는 스위퍼.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordingUploadSweeper {

    // mediamtx 컨테이너와 공유하는 녹화 볼륨의 마운트 지점
    private static final Path RECORDINGS_ROOT = Path.of("/recordings");

    private static final String S3_KEY_PREFIX = "recordings/";

    private static final String MP4_SUFFIX = ".mp4";

    private final RecordingUploadService recordingUploadService;

    // 마지막 수정 시각이 이 임계값보다 최근이면 mediamtx가 아직 쓰는 중일 수 있으므로 건너뜀 (기본 10분)
    @Value("${recording.sweep.stable-after-ms:600000}")
    private long stableAfterMs;

    /*
     * 기본 10분 간격으로 /recordings를 훑어 안정화된 .mp4를 재업로드 시도.
     * uploadResumable 내부의 파일 단위 Redis 락이 훅과의 중복 처리를 막아줌.
     */
    @Scheduled(fixedDelayString = "${recording.sweep.interval-ms:600000}")
    public void sweep() {
        // 방송자가 저장 요청한 경로만 재시도 (전체 /recordings 탐색 → 선택적 업로드)
        Set<String> pendingPaths = recordingUploadService.getPendingStreamPaths();
        if (pendingPaths.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        int scanned = 0;
        int attempted = 0;

        for (String streamPath : pendingPaths) {
            Path recordingDir = RECORDINGS_ROOT.resolve(streamPath);
            if (!Files.exists(recordingDir)) continue;

            try (var dirStream = Files.list(recordingDir)) {
                for (Path file : (Iterable<Path>) dirStream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(MP4_SUFFIX))::iterator) {
                    scanned++;
                    try {
                        long lastModified = Files.getLastModifiedTime(file).toMillis();
                        if (now - lastModified < stableAfterMs) continue;
                    } catch (IOException e) {
                        log.warn("녹화 스위퍼: 파일 상태 조회 실패 file={}", file, e);
                        continue;
                    }

                    String key = S3_KEY_PREFIX + streamPath + "/" + file.getFileName();
                    attempted++;
                    try {
                        recordingUploadService.uploadResumable(file, key);
                    } catch (Exception e) {
                        log.warn("녹화 스위퍼: 파일 업로드 중 예외, 다음 파일로 계속 진행 file={}", file, e);
                    }
                }
            } catch (IOException e) {
                log.warn("녹화 스위퍼: {} 디렉터리 탐색 실패", streamPath, e);
            }
        }

        log.info("녹화 스위퍼 완료: 스캔 {}건, 업로드 시도 {}건", scanned, attempted);
    }
}
