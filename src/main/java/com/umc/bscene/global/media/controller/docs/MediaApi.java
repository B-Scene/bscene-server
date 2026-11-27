package com.umc.bscene.global.media.controller.docs;

import com.umc.bscene.global.media.dto.request.MediaPresignedUrlRequest;
import com.umc.bscene.global.media.dto.response.MediaPresignedUrlResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.SwaggerResponseEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

public interface MediaApi {

    @Operation(tags = "미디어 업로드 (공통)", summary = "미디어 업로드 Presigned URL 발급",
            description = "5분 동안 유효한 업로드 URL을 발급합니다. presignedUrl에 파일 바이너리를 PUT하고 요청에서 보낸 contentType과 동일한 Content-Type 헤더를 사용하세요. 업로드가 끝나면 fileUrl을 콘텐츠 등록·수정 API에 전달합니다. 세션 프로필과 포트폴리오는 GIF 업로드가 제한됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MediaPresignedUrlRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "category": "POST",
                                      "fileName": "clip.mp4",
                                      "contentType": "video/mp4"
                                    }
                                    """),
                            @ExampleObject(name = "세션 프로필 이미지", value = """
                                    {
                                      "category": "SESSION_PROFILE",
                                      "fileName": "profile.png",
                                      "contentType": "image/png"
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "업로드 URL을 발급했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = PresignedUrlEnvelope.class), examples = {
                            @ExampleObject(name = "업로드 URL 발급", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "MEDIA200_1",
                                      "message": "업로드 URL을 발급했어요.",
                                      "result": {
                                        "presignedUrl": "https://bscene-example.s3.ap-northeast-2.amazonaws.com/post/9a437c83-c1e7-4d35-8ef0-fbca9cdf3764.mp4?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20261128T050000Z&X-Amz-Expires=300&X-Amz-SignedHeaders=content-type%3Bhost&X-Amz-Signature=example",
                                        "fileUrl": "https://bscene-example.s3.ap-northeast-2.amazonaws.com/post/9a437c83-c1e7-4d35-8ef0-fbca9cdf3764.mp4"
                                      },
                                      "timeStamp": "2026-11-28 14:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<MediaPresignedUrlResponse>> createPresignedUrl(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid MediaPresignedUrlRequest request
    );

    abstract class PresignedUrlEnvelope extends SwaggerResponseEnvelope<MediaPresignedUrlResponse> {
    }
}
