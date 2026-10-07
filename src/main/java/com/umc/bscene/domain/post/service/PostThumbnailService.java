package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.post.event.PostVideoThumbnailRequestedEvent;
import com.umc.bscene.global.media.enums.MediaCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// 썸네일 없이 등록된 영상 콘텐츠의 첫 프레임을 추출해 썸네일로 채워주는 후처리 (실패해도 게시물 등록 자체에는 영향 없음)
@Slf4j
@Service
@RequiredArgsConstructor
public class PostThumbnailService {

    private static final long FFMPEG_TIMEOUT_SECONDS = 30;
    private static final long MAX_VIDEO_BYTES = 500L * 1024 * 1024;

    // 게시물 미디어는 프리사인드 URL 발급 시 MediaCategory.POST 접두사로만 올라오므로, 그 아래 객체만 처리한다
    private static final String POST_MEDIA_PREFIX = MediaCategory.POST.name().toLowerCase() + "/";

    private final S3Client s3Client;
    private final PostService postService;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Async("postThumbnailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostVideoThumbnailRequestedEvent event) {
        Path videoFile = null;
        Path thumbnailFile = null;
        try {
            String key = validatedS3Key(event.videoUrl());
            validateVideoSize(key);
            videoFile = downloadVideo(key);
            thumbnailFile = extractFirstFrame(videoFile);
            String thumbnailUrl = uploadThumbnail(thumbnailFile);
            postService.applyGeneratedThumbnail(event.postId(), thumbnailUrl);
        } catch (Exception e) {
            log.error("영상 썸네일 자동 생성 실패 (postId={})", event.postId(), e);
        } finally {
            deleteQuietly(videoFile);
            deleteQuietly(thumbnailFile);
        }
    }

    /*
     * 요청 본문의 videoUrl은 사용자 입력이다. URL path를 그대로 S3 key로 쓰면 버킷 안의
     * 임의 객체(다른 유저의 녹화본·프로필 등)를 받아 썸네일로 공개할 수 있으므로,
     * 우리 버킷 호스트 + 게시물 미디어 접두사 + 영상 확장자인 경우에만 처리한다.
     */
    private String validatedS3Key(String videoUrl) {
        URI uri;
        try {
            uri = URI.create(videoUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("영상 URL 형식이 올바르지 않습니다.", e);
        }

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
        if (!key.startsWith(POST_MEDIA_PREFIX) || key.contains("/../") || !isVideoKey(key)) {
            throw new IllegalArgumentException("게시물 영상 경로가 아닙니다.");
        }
        return key;
    }

    private boolean isVideoKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mov")
                || lower.endsWith(".webm") || lower.endsWith(".m4v")
                || lower.endsWith(".avi");
    }

    // 과대 파일을 내려받아 디스크·ffmpeg 시간을 소모하지 않도록 다운로드 전에 크기를 확인한다
    private void validateVideoSize(String key) {
        long size = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()).contentLength();
        if (size <= 0 || size > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("영상 파일 크기가 허용 범위를 벗어났습니다.");
        }
    }

    private Path downloadVideo(String key) throws IOException {
        String extension = key.substring(key.lastIndexOf('.'));

        Path videoFile = Files.createTempFile("post-video-", extension);
        Files.delete(videoFile);

        s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build(),
                videoFile
        );

        return videoFile;
    }

    private Path extractFirstFrame(Path videoFile) throws IOException, InterruptedException {
        Path thumbnailFile = Files.createTempFile("post-thumbnail-", ".jpg");

        Process process = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", videoFile.toString(),
                "-frames:v", "1",
                thumbnailFile.toString()
        ).redirectErrorStream(true).start();

        boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("ffmpeg 실행이 시간 초과되었습니다.");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("ffmpeg 프레임 추출에 실패했습니다. exitCode=" + process.exitValue());
        }

        return thumbnailFile;
    }

    private String uploadThumbnail(Path thumbnailFile) {
        String key = MediaCategory.POST_THUMBNAIL.name().toLowerCase() + "/" + UUID.randomUUID() + ".jpg";

        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType("image/jpeg").build(),
                RequestBody.fromFile(thumbnailFile)
        );

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("임시 파일 삭제 실패: {}", path, e);
        }
    }
}
