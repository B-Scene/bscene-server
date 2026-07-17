package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingUploadService {

    // mediamtx 컨테이너와 공유하는 녹화 볼륨의 마운트 지점
    private static final Path RECORDINGS_ROOT = Path.of("/recordings");

    private static final String S3_KEY_PREFIX = "recordings/";

    // 멀티파트 파트 크기 8MB. S3는 마지막 파트를 제외한 모든 파트가 5MB 이상이어야 함
    private static final long PART_SIZE = 8L * 1024 * 1024;

    // 진행 중인 멀티파트 세션의 uploadId를 재시도 시 재사용하기 위해 Redis에 보관
    private static final String UPLOAD_ID_KEY_PREFIX = "recording:upload:";
    private static final Duration UPLOAD_ID_TTL = Duration.ofHours(73);

    // 훅과 스위퍼가 같은 파일을 동시에 업로드하지 못하게 막는 파일 단위 락
    private static final String LOCK_KEY_PREFIX = "recording:lock:";
    private static final Duration LOCK_TTL = Duration.ofHours(1);

    // 방송자가 다시보기 저장을 요청한 스트림 경로. 스위퍼가 이 경로만 재시도
    private static final String PENDING_KEY_PREFIX = "recording:pending:";
    private static final Duration PENDING_TTL = Duration.ofHours(73);

    // 다시보기 등록 완료 판정을 위한 스트림별 전체 세그먼트 수
    private static final String EXPECTED_COUNT_KEY_PREFIX = "recording:expected:";
    private static final Duration EXPECTED_COUNT_TTL = Duration.ofHours(73);

    // ffprobe 실행이 매달리는 것을 방지하기 위한 바운드 대기 시간
    private static final long FFPROBE_TIMEOUT_SEC = 30;

    private final S3Client s3Client;
    private final StringRedisTemplate redisTemplate;
    private final StreamReplayRepository streamReplayRepository;
    private final AudioStreamRepository audioStreamRepository;
    private final ReplayNotificationService replayNotificationService;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /*
     * MediaMTX의 runOnRecordSegmentComplete 훅으로 트리거됨.
     * 훅 요청에 즉시 200을 돌려주기 위해 업로드는 비동기로 수행.
     */
    @Async
    public void uploadAsync(String path, String segmentPath) {

        Path file = Path.of(segmentPath).normalize();

        // 내부망 전용 엔드포인트지만, 임의 경로 파일의 업로드·삭제를 막기 위해 /recordings 하위만 허용
        if (!file.startsWith(RECORDINGS_ROOT)) {
            log.warn("녹화 업로드 거부: /recordings 외부 경로 segmentPath={}", segmentPath);
            return;
        }

        // path 필드와 segmentPath 부모 디렉터리명이 달라서 엉뚱한 AudioStream에 녹화본이 붙는 것을 방지
        Path parentDir = file.getParent();
        if (parentDir == null || !parentDir.getFileName().toString().equals(path)) {
            log.warn("녹화 업로드 거부: path와 segmentPath 디렉터리 불일치 path={}, segmentPath={}", path, segmentPath);
            return;
        }

        if (!Files.exists(file)) {
            log.warn("녹화 파일이 존재하지 않음 path={}, segmentPath={}", path, segmentPath);
            return;
        }

        String key = S3_KEY_PREFIX + path + "/" + file.getFileName();
        uploadResumable(file, key);
    }

    /*
     * 중단된 지점부터 이어서 올리는 S3 멀티파트 업로드.
     * @Async self-invocation 문제 없이 스위퍼가 직접 호출할 수 있도록 패키지 가시성으로 분리.
     * uploadId는 Redis에 남고 S3가 이미 올라간 파트를 기억하므로, 재시도 시 처음부터 다시 올리지 않는다.
     */
    void uploadResumable(Path file, String s3Key) {

        String lockKey = LOCK_KEY_PREFIX + s3Key;

        // SET-if-absent로 파일 단위 락 획득. 이미 다른 워커(훅/스위퍼)가 잡고 있으면 즉시 종료
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("녹화 업로드 스킵: 다른 워커가 처리 중 key={}", s3Key);
            return;
        }

        String uploadIdKey = UPLOAD_ID_KEY_PREFIX + s3Key;

        try {
            // 1. 기존 uploadId 조회 후, 유효하면 이미 올라간 파트 목록을 가져옴
            String uploadId = redisTemplate.opsForValue().get(uploadIdKey);
            Map<Integer, CompletedPart> completed = new HashMap<>();

            if (uploadId != null) {
                try {
                    List<Part> parts = s3Client.listParts(ListPartsRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .uploadId(uploadId)
                            .build()).parts();

                    for (Part part : parts) {
                        completed.put(part.partNumber(), CompletedPart.builder()
                                .partNumber(part.partNumber())
                                .eTag(part.eTag())
                                .build());
                    }
                } catch (NoSuchUploadException e) {
                    // uploadId가 만료·중단되어 더 이상 유효하지 않음 → 새로 시작
                    log.warn("기존 멀티파트 세션이 유효하지 않아 새로 시작 key={}", s3Key);
                    uploadId = null;
                    completed.clear();
                }
            }

            // 2. 유효한 uploadId가 없으면 새 멀티파트 세션 생성 후 Redis에 저장
            if (uploadId == null) {
                CreateMultipartUploadResponse created = s3Client.createMultipartUpload(
                        CreateMultipartUploadRequest.builder()
                                .bucket(bucket)
                                .key(s3Key)
                                .contentType("audio/mp4")
                                .build());
                uploadId = created.uploadId();
                redisTemplate.opsForValue().set(uploadIdKey, uploadId, UPLOAD_ID_TTL);
            }

            long fileLength = Files.size(file);
            if (fileLength == 0) {
                log.warn("0바이트 세그먼트 스킵, 로컬 파일 삭제 key={}", s3Key);
                Files.deleteIfExists(file);
                return;
            }
            int totalParts = (int) ((fileLength + PART_SIZE - 1) / PART_SIZE);

            List<CompletedPart> completedParts = new ArrayList<>();

            // 3. 아직 안 올라간 파트만 업로드하고, 이미 올라간 파트의 ETag는 재사용
            try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                for (int partNumber = 1; partNumber <= totalParts; partNumber++) {

                    CompletedPart already = completed.get(partNumber);
                    if (already != null) {
                        completedParts.add(already);
                        continue;
                    }

                    long offset = (long) (partNumber - 1) * PART_SIZE;
                    int length = (int) Math.min(PART_SIZE, fileLength - offset);

                    byte[] buffer = new byte[length];
                    raf.seek(offset);
                    raf.readFully(buffer);

                    String eTag = s3Client.uploadPart(UploadPartRequest.builder()
                                    .bucket(bucket)
                                    .key(s3Key)
                                    .uploadId(uploadId)
                                    .partNumber(partNumber)
                                    .build(),
                            RequestBody.fromBytes(buffer)).eTag();

                    completedParts.add(CompletedPart.builder()
                            .partNumber(partNumber)
                            .eTag(eTag)
                            .build());
                }
            }

            // 4. 파트 번호 순으로 정렬 후 멀티파트 업로드 완료
            completedParts.sort(Comparator.comparingInt(CompletedPart::partNumber));

            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build());

            // 5. 성공 시에만 다시보기 메타데이터 저장 후 Redis uploadId 키와 로컬 파일 정리
            //    순서 중요: duration(파일 필요) → 저장 → 파일 삭제
            persistReplayIfAbsent(file, s3Key);

            redisTemplate.delete(uploadIdKey);
            Files.deleteIfExists(file);
            tryCompleteReplay(extractStreamPath(s3Key));
            log.info("녹화 S3 멀티파트 업로드 완료 key={}", s3Key);

        } catch (Exception e) {
            // SdkClientException(네트워크 단절), DataAccessException(Redis/DB 장애) 등 런타임 예외 포함.
            // 재시도 여지를 남기기 위해 멀티파트 abort·로컬 파일 삭제·uploadId 키 삭제를 하지 않음.
            // 실제 재시도는 스위퍼가 담당하므로 "재시도 예정"으로만 로깅
            log.error("녹화 S3 멀티파트 업로드 실패, 스위퍼가 이어서 재시도함 key={}", s3Key, e);
        } finally {
            // 락은 항상 해제
            redisTemplate.delete(lockKey);
        }
    }

    /*
     * 업로드 성공 시 다시보기 메타데이터(StreamReplay)를 저장한다.
     * s3Key "recordings/{path}/{filename}"에서 {path}를 파싱하고, ffprobe로 재생 길이를 구한 뒤 저장한다.
     * 파일 삭제 전에 호출되어야 한다(ffprobe가 로컬 파일을 필요로 함).
     */
    private void persistReplayIfAbsent(Path file, String s3Key) {
        // ffprobe(최대 30초 프로세스) 실행 전에 이미 저장된 키인지 먼저 확인해 불필요한 CPU 낭비 방지
        if (streamReplayRepository.existsByS3Key(s3Key))
            return;

        String path = extractStreamPath(s3Key);
        int durationSec = probeDurationSec(file);

        AudioStream audioStream = audioStreamRepository.findByPath(path).orElse(null);
        if (audioStream == null) {
            // 업로드는 성공했으나 메타데이터를 붙일 AudioStream이 없음 → 파일 삭제는 그대로 진행
            log.warn("다시보기 메타데이터 저장 스킵: path에 해당하는 AudioStream 없음 path={}, key={}", path, s3Key);
            return;
        }

        try {
            streamReplayRepository.saveAndFlush(StreamReplay.builder()
                    .audioStream(audioStream)
                    .s3Key(s3Key)
                    .durationSec(durationSec)
                    .viewCount(0L)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 훅+스위퍼 동시 저장 혹은 공동 멤버 중복 저장 시의 unique 충돌 → 멱등하게 무시
            log.info("이미 저장된 다시보기 (동시 요청) key={}", s3Key);
        }
    }

    /*
     * s3Key "recordings/{path}/{filename}"에서 {path}를 추출한다.
     * 접두사(recordings/)와 마지막 '/' 사이의 구간이 {path}.
     */
    public void markPending(String streamPath, int expectedSegmentCount) {
        redisTemplate.opsForValue().set(PENDING_KEY_PREFIX + streamPath, "1", PENDING_TTL);
        redisTemplate.opsForValue().set(
                EXPECTED_COUNT_KEY_PREFIX + streamPath,
                String.valueOf(expectedSegmentCount),
                EXPECTED_COUNT_TTL
        );
    }

    /*
     * 저장된 다시보기 메타데이터 수가 요청 당시 전체 세그먼트 수에 도달하면 등록 완료 알림을 발송한다.
     * 알림 처리 중 일시 오류가 발생하면 pending 키를 유지해 스위퍼가 다시 확인한다.
     */
    void tryCompleteReplay(String streamPath) {
        String expectedCountValue =
                redisTemplate.opsForValue().get(EXPECTED_COUNT_KEY_PREFIX + streamPath);

        if (expectedCountValue == null) {
            return;
        }

        try {
            long expectedCount = Long.parseLong(expectedCountValue);
            AudioStream audioStream = audioStreamRepository.findByPath(streamPath).orElse(null);

            if (audioStream == null
                    || streamReplayRepository.countByAudioStream_Id(audioStream.getId()) < expectedCount) {
                return;
            }

            replayNotificationService.notifyReplayReady(audioStream);
            redisTemplate.delete(List.of(
                    PENDING_KEY_PREFIX + streamPath,
                    EXPECTED_COUNT_KEY_PREFIX + streamPath
            ));
        } catch (NumberFormatException exception) {
            log.warn("다시보기 예상 세그먼트 수 파싱 실패 path={}, value={}",
                    streamPath, expectedCountValue, exception);
            redisTemplate.delete(EXPECTED_COUNT_KEY_PREFIX + streamPath);
        } catch (RuntimeException exception) {
            log.warn("다시보기 등록 완료 알림 처리 실패, 스위퍼가 재시도함 path={}",
                    streamPath, exception);
        }
    }

    /*
     * streamPath에 해당하는 /recordings/{streamPath}/ 디렉터리의 .mp4 파일 목록을 반환한다.
     * 디렉터리가 없거나 IO 오류 시 빈 리스트 반환.
     */
    public List<Path> findSegments(String streamPath) {
        Path recordingDir = RECORDINGS_ROOT.resolve(streamPath);
        try (var dirStream = Files.list(recordingDir)) {
            return dirStream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".mp4"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /*
     * Redis SCAN으로 pending 키를 조회해 스트림 경로 목록을 반환한다.
     * 스위퍼가 재시도할 경로를 결정하기 위해 호출.
     */
    Set<String> getPendingStreamPaths() {
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(PENDING_KEY_PREFIX + "*").count(100).build())) {
            while (cursor.hasNext()) keys.add(cursor.next());
        }
        return keys.stream()
                .map(k -> k.substring(PENDING_KEY_PREFIX.length()))
                .collect(Collectors.toSet());
    }

    private String extractStreamPath(String s3Key) {
        String withoutPrefix = s3Key.substring(S3_KEY_PREFIX.length());
        int lastSlash = withoutPrefix.lastIndexOf('/');
        return lastSlash < 0 ? withoutPrefix : withoutPrefix.substring(0, lastSlash);
    }

    /*
     * ffprobe로 완성된 파일의 재생 길이(초)를 추출한다.
     * 인젝션 방지를 위해 셸 문자열이 아닌 인자 배열로 실행한다.
     * 실패(비정상 종료, 타임아웃, 파싱 오류, IO/Interrupt) 시 warn 로그 후 0 반환(업로드를 실패시키지 않음).
     */
    private int probeDurationSec(Path file) {
        Process process = null;
        try {
            process = new ProcessBuilder(
                    "ffprobe", "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    file.toAbsolutePath().toString()
            ).redirectErrorStream(false).start();

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.readLine();
            }

            boolean finished = process.waitFor(FFPROBE_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffprobe 타임아웃 file={}", file);
                return 0;
            }

            if (process.exitValue() != 0) {
                log.warn("ffprobe 비정상 종료 exitCode={}, file={}", process.exitValue(), file);
                return 0;
            }

            if (output == null || output.isBlank()) {
                log.warn("ffprobe 결과가 비어있음 file={}", file);
                return 0;
            }

            return Math.round(Float.parseFloat(output.trim()));
        } catch (NumberFormatException e) {
            log.warn("ffprobe 결과 파싱 실패 file={}", file, e);
            return 0;
        } catch (IOException e) {
            log.warn("ffprobe 실행 실패 file={}", file, e);
            return 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("ffprobe 대기 중 인터럽트 file={}", file, e);
            return 0;
        } finally {
            if (process != null && process.isAlive())
                process.destroyForcibly();
        }
    }
}
