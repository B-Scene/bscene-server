package com.umc.bscene.domain.post.controller.docs;

import com.umc.bscene.domain.post.dto.request.PostCommentRequest;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse;
import com.umc.bscene.domain.post.dto.response.PostCommentResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.ExploreApiResponseSchemas;
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

public interface PostCommentApi {

    @Operation(tags = "팬모드_탐색", summary = "게시물 댓글 목록 조회",
            description = "등록순으로 조회합니다. 첫 페이지의 myComments에 내 댓글을 담고, items에서 내 댓글과 차단한 사용자의 댓글을 제외합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "댓글 목록을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.PostCommentListResponseEnvelope.class), examples = {
                            @ExampleObject(name = "첫 페이지", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_4",
                                      "message": "댓글 목록을 조회했습니다.",
                                      "result": {
                                        "myComments": [
                                          {
                                            "commentId": 12,
                                            "writerMode": "FAN",
                                            "nickname": "세진",
                                            "profileImageUrl": null,
                                            "content": "공연 잘 봤어요! 다음 곡도 기대할게요.",
                                            "createdAt": "2026-07-22T21:00:00"
                                          }
                                        ],
                                        "items": [
                                          {
                                            "commentId": 3,
                                            "writerMode": "FAN",
                                            "nickname": "정하람",
                                            "profileImageUrl": "https://cdn.bscene.com/user_profile/haram.jpg",
                                            "content": "이번 편곡 진짜 기대돼요! 공연도 꼭 보러 갈게요.",
                                            "createdAt": "2026-07-22T19:40:00"
                                          },
                                          {
                                            "commentId": 4,
                                            "writerMode": "BAND",
                                            "nickname": "왕감자",
                                            "profileImageUrl": "https://cdn.bscene.com/band_profile/wavy.jpg",
                                            "content": "와주셔서 감사했습니다! 다음 공연도 기대해주세요.",
                                            "createdAt": "2026-07-22T20:00:00"
                                          },
                                          {
                                            "commentId": 5,
                                            "writerMode": "FAN",
                                            "nickname": "이지수",
                                            "profileImageUrl": null,
                                            "content": "합주 분위기가 너무 좋아 보여요. 새 곡도 기다릴게요.",
                                            "createdAt": "2026-07-22T20:15:00"
                                          }
                                        ],
                                        "hasNext": true,
                                        "nextCursor": 5
                                      },
                                      "timeStamp": "2026-07-22 22:40:11"
                                    }
                                    """),
                            @ExampleObject(name = "다음 페이지", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_4",
                                      "message": "댓글 목록을 조회했습니다.",
                                      "result": {
                                        "myComments": [],
                                        "items": [
                                          {
                                            "commentId": 8,
                                            "writerMode": "FAN",
                                            "nickname": "김리아",
                                            "profileImageUrl": null,
                                            "content": "홍대 공연 저도 갔었는데 최고였어요.",
                                            "createdAt": "2026-07-22T20:30:00"
                                          }
                                        ],
                                        "hasNext": false,
                                        "nextCursor": null
                                      },
                                      "timeStamp": "2026-07-22 22:40:11"
                                    }
                                    """),
                            @ExampleObject(name = "댓글 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_4",
                                      "message": "댓글 목록을 조회했습니다.",
                                      "result": {
                                        "myComments": [],
                                        "items": [],
                                        "hasNext": false,
                                        "nextCursor": null
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
    ResponseEntity<SuccessResponse<PostCommentListResponse>> getComments(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "게시물 ID", example = "1") Long postId,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지는 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본값은 10이며 1~30 범위로 제한됩니다.") Integer size
    );

    @Operation(tags = "팬모드_탐색", summary = "게시물 댓글 작성",
            description = "댓글은 공백만으로 작성할 수 없으며 최대 500자까지 입력할 수 있습니다. 밴드 모드에서는 소속 밴드의 게시물에만 작성할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostCommentRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "content": "공연 잘 봤어요! 다음 곡도 기대할게요."
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "댓글을 등록했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.PostCommentResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_5",
                                      "message": "댓글을 등록했어요.",
                                      "result": {
                                        "commentId": 12,
                                        "content": "공연 잘 봤어요! 다음 곡도 기대할게요.",
                                        "createdAt": "2026-07-22T21:00:00"
                                      },
                                      "timeStamp": "2026-07-22 22:40:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST403_3", ref = "#/components/examples/POST403_3"),
                            @ExampleObject(name = "POST403_4", ref = "#/components/examples/POST403_4")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST404_3", ref = "#/components/examples/POST404_3"),
                            @ExampleObject(name = "POST404_1", ref = "#/components/examples/POST404_1")
                    }))
    })
    ResponseEntity<SuccessResponse<PostCommentResponse>> createComment(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "게시물 ID", example = "1") Long postId,
            @Valid PostCommentRequest request
    );

    @Operation(tags = "팬모드_탐색", summary = "게시물 댓글 수정",
            description = "본인이 작성한 댓글의 내용을 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostCommentRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "content": "공연 정말 잘 봤어요! 다음 곡도 기대할게요."
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "댓글을 수정했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.PostCommentResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_6",
                                      "message": "댓글을 수정했어요.",
                                      "result": {
                                        "commentId": 12,
                                        "content": "공연 정말 잘 봤어요! 다음 곡도 기대할게요.",
                                        "createdAt": "2026-07-22T21:00:00"
                                      },
                                      "timeStamp": "2026-07-22 22:40:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST403_2", ref = "#/components/examples/POST403_2")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST404_2", ref = "#/components/examples/POST404_2")
                    }))
    })
    ResponseEntity<SuccessResponse<PostCommentResponse>> updateComment(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "게시물 ID", example = "1") Long postId,
            @Parameter(description = "댓글 ID", example = "12") Long commentId,
            @Valid PostCommentRequest request
    );

    @Operation(tags = "팬모드_탐색", summary = "게시물 댓글 삭제",
            description = "본인이 작성한 댓글을 삭제합니다. 응답의 content와 createdAt은 null입니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "댓글을 삭제했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.PostCommentResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_7",
                                      "message": "댓글을 삭제했어요.",
                                      "result": {
                                        "commentId": 12,
                                        "content": null,
                                        "createdAt": null
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
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST403_2", ref = "#/components/examples/POST403_2")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "POST404_2", ref = "#/components/examples/POST404_2")
                    }))
    })
    ResponseEntity<SuccessResponse<PostCommentResponse>> deleteComment(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "게시물 ID", example = "1") Long postId,
            @Parameter(description = "댓글 ID", example = "12") Long commentId
    );
}
