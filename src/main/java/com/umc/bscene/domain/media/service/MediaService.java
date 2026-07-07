package com.umc.bscene.domain.media.service;

import com.umc.bscene.domain.media.dto.request.MediaPresignedUrlRequest;
import com.umc.bscene.domain.media.dto.response.MediaPresignedUrlResponse;
import com.umc.bscene.domain.media.enums.MediaCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    // 업로드 창구가 오래 열려있지 않도록 짧게 제한
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(5);

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    public MediaPresignedUrlResponse createPresignedUrl(MediaPresignedUrlRequest request) {
        String key = generateKey(request.category(), request.fileName());

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);

        return new MediaPresignedUrlResponse(presignedRequest.url().toString(), fileUrl);
    }

    private String generateKey(MediaCategory category, String fileName) {
        String extension = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.'))
                : "";

        return category.name().toLowerCase() + "/" + UUID.randomUUID() + extension;
    }
}
