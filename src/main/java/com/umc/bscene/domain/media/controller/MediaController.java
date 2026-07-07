package com.umc.bscene.domain.media.controller;

import com.umc.bscene.domain.media.dto.request.MediaPresignedUrlRequest;
import com.umc.bscene.domain.media.dto.response.MediaPresignedUrlResponse;
import com.umc.bscene.domain.media.response.code.MediaSuccessCode;
import com.umc.bscene.domain.media.service.MediaService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    // 파일 업로드용 프리사인드 URL 발급 (클라이언트가 이 URL로 S3에 직접 PUT 후, fileUrl을 콘텐츠 등록/수정 API에 전달)
    @PostMapping("/media/presigned-url")
    public ResponseEntity<SuccessResponse<MediaPresignedUrlResponse>> createPresignedUrl(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MediaPresignedUrlRequest request
    ) {
        MediaPresignedUrlResponse response = mediaService.createPresignedUrl(request);
        SuccessResponse<MediaPresignedUrlResponse> successResponse = SuccessResponse.of(
                response,
                MediaSuccessCode.PRESIGNED_URL_CREATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
