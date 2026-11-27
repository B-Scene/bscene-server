package com.umc.bscene.domain.post.controller.docs;

import com.umc.bscene.domain.post.dto.response.PostDetailResponse;
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

public interface ExplorePostApi {

    @Operation(tags = "팬모드_탐색", summary = "게시물 상세 페이지 조회",
            description = "작성 밴드, 미디어, 좋아요 및 댓글 수를 조회합니다. 댓글 목록은 별도 API로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "콘텐츠 상세페이지를 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.PostDetailResponseEnvelope.class), examples = {
                            @ExampleObject(name = "사진 게시물", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_3",
                                      "message": "콘텐츠 상세페이지를 조회했습니다.",
                                      "result": {
                                        "postId": 1,
                                        "band": {
                                          "bandId": 1,
                                          "name": "WAVY",
                                          "profileImageUrl": "https://cdn.bscene.com/band-profile.jpg"
                                        },
                                        "type": "PHOTO",
                                        "mediaUrls": [
                                          "https://cdn.bscene.com/post/photo1.jpg",
                                          "https://cdn.bscene.com/post/photo2.jpg"
                                        ],
                                        "thumbnailUrl": null,
                                        "title": "2025 봄 정기공연 합주 기록",
                                        "description": "다음 공연을 앞두고 멤버들과 합주를 진행했어요.\\n새롭게 편곡한 곡과 라이브 셋리스트를 맞춰보며\\n무대에서 더 좋은 사운드를 들려드릴 준비를 하고 있습니다.",
                                        "tags": [
                                          "합주",
                                          "인디밴드",
                                          "홍대",
                                          "공연준비"
                                        ],
                                        "createdAt": "2026-07-22T20:10:00",
                                        "likeCount": 411,
                                        "commentCount": 78,
                                        "isLiked": false
                                      },
                                      "timeStamp": "2026-07-22 22:40:11"
                                    }
                                    """),
                            @ExampleObject(name = "영상 게시물", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "POST200_3",
                                      "message": "콘텐츠 상세페이지를 조회했습니다.",
                                      "result": {
                                        "postId": 2,
                                        "band": {
                                          "bandId": 1,
                                          "name": "WAVY",
                                          "profileImageUrl": "https://cdn.bscene.com/band-profile.jpg"
                                        },
                                        "type": "VIDEO",
                                        "mediaUrls": [
                                          "https://cdn.bscene.com/post/rehearsal.mp4"
                                        ],
                                        "thumbnailUrl": "https://cdn.bscene.com/post_thumbnail/thumb.jpg",
                                        "title": "신곡 합주 영상",
                                        "description": "새 곡 일부를 먼저 공개합니다!",
                                        "tags": [],
                                        "createdAt": "2026-07-22T18:00:00",
                                        "likeCount": 0,
                                        "commentCount": 0,
                                        "isLiked": false
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
    ResponseEntity<SuccessResponse<PostDetailResponse>> getPostDetailPage(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "게시물 ID", example = "1") Long postId
    );
}
