package com.umc.bscene.domain.media.dto.response;

public record MediaPresignedUrlResponse(
        // 클라이언트가 이 URL로 파일을 직접 PUT 업로드
        String presignedUrl,
        // 업로드 완료 후 실제 저장에 사용할 최종 S3 공개 URL
        String fileUrl
) {
}
