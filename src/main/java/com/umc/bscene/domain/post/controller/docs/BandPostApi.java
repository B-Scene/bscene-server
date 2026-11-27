package com.umc.bscene.domain.post.controller.docs;

import com.umc.bscene.domain.post.dto.request.PostCreateRequest;
import com.umc.bscene.domain.post.dto.request.PostUpdateRequest;
import com.umc.bscene.domain.post.dto.response.PostCreateResponse;
import com.umc.bscene.domain.post.dto.response.PostListResponse;
import com.umc.bscene.domain.post.dto.response.PostResponse;
import com.umc.bscene.domain.post.dto.response.PostUpdateResponse;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import org.springframework.http.ResponseEntity;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.swagger.BandContentResponseSchemas;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

public interface BandPostApi {

    @Operation(tags = "밴드모드_내밴드(게시물)", summary = "게시물 작성",
            description = "검수를 통과한 밴드의 멤버가 게시물을 작성합니다. 태그는 최대 8개입니다. 사진은 미디어가 1개 이상, 영상은 1개, 글은 0개여야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PostCreateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "type": "PHOTO",
                                              "title": "2025 봄 정기공연 준비",
                                              "description": "정기 공연을 위한 합주 연습 현장을 공유합니다",
                                              "mediaUrls": ["https://cdn.bscene.com/post/1.png","https://cdn.bscene.com/post/2.png"],
                                              "tags": ["인디팝","공연준비"],
                                              "thumbnailUrl": "https://cdn.bscene.com/post/1.png"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.PostCreationEnvelope.class), examples = {
                                    @ExampleObject(name = "POST201_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "POST201_1",
                                              "message": "콘텐츠가 업로드됐어요.",
                                              "result": {"postId":100,"type":"PHOTO","title":"2025 봄 정기공연 준비","description":"정기 공연을 위한 합주 연습 현장을 공유합니다","mediaUrls":["https://cdn.bscene.com/post/1.png","https://cdn.bscene.com/post/2.png"],"tags":["인디팝","공연준비"],"createdAt":"2025-08-01T14:20:11","thumbnailUrl":"https://cdn.bscene.com/post/1.png"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "POST400_1", ref = "#/components/examples/POST400_1"),
                                    @ExampleObject(name = "POST400_2", ref = "#/components/examples/POST400_2"),
                                    @ExampleObject(name = "POST400_3", ref = "#/components/examples/POST400_3"),
                                    @ExampleObject(name = "POST400_4", ref = "#/components/examples/POST400_4"),
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "POST403_1", ref = "#/components/examples/POST403_1"),
                                    @ExampleObject(name = "BAND403_6", ref = "#/components/examples/BAND403_6")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PostCreateResponse>> createPost(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            @Valid PostCreateRequest request
    );

    @Operation(tags = "밴드모드_내밴드(게시물)", summary = "밴드 게시물 목록",
            description = "콘텐츠 유형으로 필터링할 수 있습니다. 다음 페이지에는 nextCursor를 cursor로 전달합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.PostListEnvelope.class), examples = {
                                    @ExampleObject(name = "POST200_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "POST200_1",
                                              "message": "콘텐츠 목록을 조회했습니다.",
                                              "result": {"posts":[{"postId":2,"type":"PHOTO","title":"2025 봄 정기공연 준비","description":"정기 공연을 위한 합주 연습 현장을 공유합니다","thumbnailUrl":"https://cdn.bscene.com/post/1.png","createdAt":"2026-07-21T21:39:53.668983"},{"postId":1,"type":"PHOTO","title":"2025 봄 정기공연 준비","description":"정기 공연을 위한 합주 연습 현장을 공유합니다","thumbnailUrl":"https://cdn.bscene.com/post/1.png","createdAt":"2026-07-21T21:39:52.07939"}],"hasNext":false,"nextCursor":null},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PostListResponse>> getPosts(
            @Parameter(example = "1") Long bandId,
            @Parameter(example = "PHOTO") PostType type,
            @Parameter(example = "100") Long cursor,
            @Parameter(example = "10") Integer size
    );

    @Operation(tags = "밴드모드_내밴드(게시물)", summary = "게시물 상세",
            description = "본문, 미디어, 썸네일, 태그를 조회합니다. 수정 화면의 초기 데이터로 사용할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.PostDetailEnvelope.class), examples = {
                                    @ExampleObject(name = "POST200_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "POST200_2",
                                              "message": "콘텐츠를 조회했습니다.",
                                              "result": {"postId":100,"bandId":1,"bandName":"APPLE","type":"PHOTO","title":"2025 봄 정기공연 준비","description":"정기 공연을 위한 합주 연습 현장을 공유합니다","mediaUrls":["https://cdn.bscene.com/post/1.png","https://cdn.bscene.com/post/2.png"],"thumbnailUrl":"https://cdn.bscene.com/post/1.png","tags":["인디팝","공연준비"],"createdAt":"2025-08-01T14:20:11","updatedAt":"2025-08-01T14:20:11"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "POST404_1", ref = "#/components/examples/POST404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PostResponse>> getPostDetail(
            @Parameter(example = "100") Long postId
    );

    @Operation(tags = "밴드모드_내밴드(게시물)", summary = "게시물 수정",
            description = "소속 밴드 게시물을 수정합니다. 미디어와 태그를 전달하면 해당 목록을 교체하고, 생략하면 유지합니다. 게시물 유형은 유지됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PostUpdateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "title": "2025 봄 정기공연 준비 (수정)",
                                              "description": "설명 수정합니다",
                                              "mediaUrls": ["https://cdn.bscene.com/post/1_new.png"],
                                              "tags": ["인디팝","공연","홍대"]
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.PostUpdateEnvelope.class), examples = {
                                    @ExampleObject(name = "POST201_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "POST201_2",
                                              "message": "콘텐츠가 수정됐어요.",
                                              "result": {"postId":100,"type":"PHOTO","title":"2025 봄 정기공연 준비 (수정)","description":"설명 수정합니다","mediaUrls":["https://cdn.bscene.com/post/1_new.png"],"thumbnailUrl":"https://cdn.bscene.com/post/1.png","tags":["인디팝","공연","홍대"],"updatedAt":"2025-08-01T15:00:00"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "POST400_1", ref = "#/components/examples/POST400_1"),
                                    @ExampleObject(name = "POST400_2", ref = "#/components/examples/POST400_2"),
                                    @ExampleObject(name = "POST400_3", ref = "#/components/examples/POST400_3"),
                                    @ExampleObject(name = "POST400_4", ref = "#/components/examples/POST400_4"),
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "POST403_1", ref = "#/components/examples/POST403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "POST404_1", ref = "#/components/examples/POST404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PostUpdateResponse>> updatePost(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "100") Long postId,
            PostUpdateRequest request
    );

    @Operation(tags = "밴드모드_내밴드(게시물)", summary = "게시물 삭제",
            description = "소속 밴드의 게시물을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                                    @ExampleObject(name = "POST201_3", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "POST201_3",
                                              "message": "콘텐츠가 삭제됐어요.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "POST403_1", ref = "#/components/examples/POST403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "POST404_1", ref = "#/components/examples/POST404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> deletePost(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "100") Long postId
    );
}
