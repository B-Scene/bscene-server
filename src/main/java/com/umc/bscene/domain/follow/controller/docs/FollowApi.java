package com.umc.bscene.domain.follow.controller.docs;

import com.umc.bscene.domain.follow.dto.response.FollowResponse;
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

public interface FollowApi {

    @Operation(tags = "팬모드_탐색", summary = "밴드 팔로우",
            description = "팬 모드에서 밴드를 팔로우합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "201", description = "밴드를 팔로우했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.FollowResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "FOLLOW201_1",
                                      "message": "밴드를 팔로우했습니다.",
                                      "result": {
                                        "bandId": 5,
                                        "isFollowing": true
                                      },
                                      "timeStamp": "2026-07-04 04:20:11"
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
                            @ExampleObject(name = "FOLLOW403_1", ref = "#/components/examples/FOLLOW403_1"),
                            @ExampleObject(name = "FOLLOW403_2", ref = "#/components/examples/FOLLOW403_2")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "FOLLOW409_1", ref = "#/components/examples/FOLLOW409_1")
                    }))
    })
    ResponseEntity<SuccessResponse<FollowResponse>> follow(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "밴드 ID", example = "1") Long bandId
    );

    @Operation(tags = "팬모드_탐색", summary = "밴드 팔로우 취소",
            description = "팔로우를 취소합니다. 이미 취소된 상태에서도 성공합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "밴드 팔로우를 취소했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.FollowResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "FOLLOW200_1",
                                      "message": "밴드 팔로우를 취소했습니다.",
                                      "result": {
                                        "bandId": 5,
                                        "isFollowing": false
                                      },
                                      "timeStamp": "2026-07-04 04:30:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<FollowResponse>> unfollow(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "밴드 ID", example = "1") Long bandId
    );
}
