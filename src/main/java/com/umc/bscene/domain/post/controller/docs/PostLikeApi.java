package com.umc.bscene.domain.post.controller.docs;

import com.umc.bscene.domain.post.dto.response.PostLikeResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.ExploreApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

public interface PostLikeApi {

    @Operation(tags = "팬모드_탐색", summary = "게시물 좋아요 등록",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "좋아요를 등록했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.PostLikeResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_8",
                                      "message": "좋아요를 등록했어요.",
                                      "result": {
                                        "postId": 1,
                                        "isLiked": true,
                                        "likeCount": 412
                                      },
                                      "timeStamp": "2026-07-22 22:40:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST404_1", ref = "#/components/examples/POST404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST409_1", ref = "#/components/examples/POST409_1")
                    }))
    })
    ResponseEntity<SuccessResponse<PostLikeResponse>> setLike(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "게시물 ID", example = "1") Long postId
    );

    @Operation(tags = "팬모드_탐색", summary = "게시물 좋아요 해제",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "좋아요를 해제했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.PostLikeResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_9",
                                      "message": "좋아요를 해제했어요.",
                                      "result": {
                                        "postId": 1,
                                        "isLiked": false,
                                        "likeCount": 411
                                      },
                                      "timeStamp": "2026-07-22 22:40:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST404_1", ref = "#/components/examples/POST404_1")
                    }))
    })
    ResponseEntity<SuccessResponse<PostLikeResponse>> unsetLike(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "게시물 ID", example = "1") Long postId
    );
}
