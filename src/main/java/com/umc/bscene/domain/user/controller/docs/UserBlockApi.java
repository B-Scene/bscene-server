package com.umc.bscene.domain.user.controller.docs;

import com.umc.bscene.domain.user.dto.request.UserBlockRequest;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public interface UserBlockApi {

    @Operation(tags = "팬모드_라이브", summary = "특정 사용자 차단",
            description = "특정 사용자를 이 라이브에서 차단합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserBlockRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "liveId": 1,
                                      "targetUserId": 2
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "사용자를 차단했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "USER200_4",
                                      "message": "사용자를 차단했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "USER400_1", ref = "#/components/examples/USER400_1")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "USER404_2", ref = "#/components/examples/USER404_2"),
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "USER409_1", ref = "#/components/examples/USER409_1")
                    }))
    })
    SuccessResponse<Void> block(
            @Parameter(hidden = true) AuthMember authMember,
            UserBlockRequest request
    );

    @Operation(tags = "팬모드_라이브", summary = "특정 사용자 차단해제",
            description = "이 라이브에서 특정 사용자의 차단을 해제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserBlockRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "liveId": 1,
                                      "targetUserId": 2
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "사용자 차단을 해제했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "USER200_5",
                                      "message": "사용자 차단을 해제했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "USER400_1", ref = "#/components/examples/USER400_1")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "USER404_3", ref = "#/components/examples/USER404_3")
                    }))
    })
    SuccessResponse<Void> unblock(
            @Parameter(hidden = true) AuthMember authMember,
            UserBlockRequest request
    );
}
