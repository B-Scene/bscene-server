package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.repository.SessionApplicationLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionPortfolioVideoThumbnailService {
    private static final long MAX_VIDEO_BYTES = 500L * 1024 * 1024;
    private static final long FFMPEG_TIMEOUT_SECONDS = 30;
    private static final String PORTFOLIO_PREFIX = "session_portfolio/";
    private static final String THUMBNAIL_PREFIX = PORTFOLIO_PREFIX + "thumbnails/";

    private final S3Client s3Client;
    private final SessionApplicationLinkRepository linkRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Async("sessionPortfolioPreviewExecutor")
    public void generateAsync(Long linkId, String videoUrl) {
        Path videoFile = null;
        Path thumbnailFile = null;
        try {
            String key = validatedS3Key(videoUrl);
            validateVideoSize(key);
            videoFile = downloadVideo(key);
            thumbnailFile = extractFrame(videoFile);
            String thumbnailUrl = uploadThumbnail(thumbnailFile);
            saveThumbnail(linkId, videoUrl, thumbnailUrl);
        } catch (Exception exception) {
            log.warn("세션 포트폴리오 영상 썸네일 생성 실패: linkId={}, url={}",
                    linkId, videoUrl, exception);
        } finally {
            deleteQuietly(videoFile);
            deleteQuietly(thumbnailFile);
        }
    }

    private String validatedS3Key(String videoUrl) {
        URI uri = URI.create(videoUrl);
        String expectedHost = bucket + ".s3." + region + ".amazonaws.com";
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !uri.getHost().equalsIgnoreCase(expectedHost)
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("우리 S3에 업로드된 영상만 처리할 수 있습니다.");
        }
        String path = uri.getPath();
        if (path == null || path.length() <= 1) {
            throw new IllegalArgumentException("S3 객체 경로가 없습니다.");
        }
        String key = path.substring(1);
        if (!key.startsWith(PORTFOLIO_PREFIX) || !isVideoKey(key)) {
            throw new IllegalArgumentException("세션 포트폴리오 영상 경로가 아닙니다.");
        }
        return key;
    }

    private void validateVideoSize(String key) {
        long size = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()).contentLength();
        if (size <= 0 || size > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("영상 파일 크기가 허용 범위를 벗어났습니다.");
        }
    }

    private Path downloadVideo(String key) throws Exception {
        String extension = key.substring(key.lastIndexOf('.'));
        Path target = Files.createTempFile("session-portfolio-video-", extension);
        Files.deleteIfExists(target);
        s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(), target);
        return target;
    }

    private Path extractFrame(Path videoFile) throws Exception {
        Path thumbnail = Files.createTempFile("session-portfolio-thumbnail-", ".jpg");
        Process process = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", videoFile.toString(),
                "-frames:v", "1",
                "-q:v", "2",
                thumbnail.toString()
        ).redirectErrorStream(true).start();

        boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("ffmpeg 실행 시간이 초과되었습니다.");
        }
        if (process.exitValue() != 0 || Files.size(thumbnail) == 0) {
            throw new IllegalStateException(
                    "영상 프레임 추출에 실패했습니다. exitCode=" + process.exitValue());
        }
        return thumbnail;
    }

    private String uploadThumbnail(Path thumbnailFile) {
        String key = THUMBNAIL_PREFIX + UUID.randomUUID() + ".jpg";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromFile(thumbnailFile)
        );
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private void saveThumbnail(Long linkId, String expectedUrl, String thumbnailUrl) {
        transactionTemplate.executeWithoutResult(status -> linkRepository.findById(linkId)
                .filter(link -> link.getDeletedAt() == null)
                .filter(link -> link.getUrl().equals(expectedUrl))
                .ifPresent(link -> link.applyGeneratedThumbnail(thumbnailUrl)));
    }

    private boolean isVideoKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mov")
                || lower.endsWith(".webm") || lower.endsWith(".m4v")
                || lower.endsWith(".avi");
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception exception) {
            log.warn("세션 포트폴리오 임시 파일 삭제 실패: {}", path, exception);
        }
    }
}
