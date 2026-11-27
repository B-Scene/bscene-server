package com.umc.bscene.domain.performance.controller.docs;

import com.umc.bscene.domain.performance.dto.response.PerformanceInterestResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.FanHomeApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

public interface PerformanceInterestApi {

    @Operation(tags = "팬모드_홈", summary = "관심 공연 등록",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "201", description = "관심 공연으로 등록했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PerformanceInterestResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "SHOW201_3",
                                      "message": "관심 공연으로 등록했어요.",
                                      "result": {
                                        "performanceId": 1,
                                        "isInterested": true
                                      },
                                      "timeStamp": "2026-07-10 15:40:11"
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
                            @ExampleObject(name = "SHOW404_1", ref = "#/components/examples/SHOW404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SHOW409_2", ref = "#/components/examples/SHOW409_2")
                    }))
    })
    ResponseEntity<SuccessResponse<PerformanceInterestResponse>> setInterest(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "공연 ID", example = "1") Long performanceId
    );

    @Operation(tags = "팬모드_홈", summary = "관심 공연 해제",
            description = "관심 공연 등록을 해제합니다. 이미 해제된 경우에도 성공합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "관심 공연을 해제했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PerformanceInterestResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SHOW200_6",
                                      "message": "관심 공연을 해제했어요.",
                                      "result": {
                                        "performanceId": 1,
                                        "isInterested": false
                                      },
                                      "timeStamp": "2026-07-10 15:42:41"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PerformanceInterestResponse>> unsetInterest(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "공연 ID", example = "1") Long performanceId
    );
}
