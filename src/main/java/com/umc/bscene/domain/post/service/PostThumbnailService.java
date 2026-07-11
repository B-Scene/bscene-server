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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// 썸네일 없이 등록된 영상 콘텐츠의 첫 프레임을 추출해 썸네일로 채워주는 후처리 (실패해도 게시물 등록 자체에는 영향 없음)
@Slf4j
@Service
@RequiredArgsConstructor
public class PostThumbnailService {

    private static final long FFMPEG_TIMEOUT_SECONDS = 30;

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
            videoFile = downloadVideo(event.videoUrl());
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

    private Path downloadVideo(String videoUrl) throws IOException {
        String key = URI.create(videoUrl).getPath().substring(1);
        String extension = key.contains(".") ? key.substring(key.lastIndexOf('.')) : ".tmp";

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
