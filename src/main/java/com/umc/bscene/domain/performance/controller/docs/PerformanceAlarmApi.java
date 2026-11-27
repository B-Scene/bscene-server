package com.umc.bscene.domain.performance.controller.docs;

import com.umc.bscene.domain.performance.dto.response.PerformanceAlarmResponse;
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

public interface PerformanceAlarmApi {

    @Operation(tags = "팬모드_홈", summary = "공연 알림 설정",
            description = "공연 시작 전 알림을 설정하면 참여 예정 상태가 되고 관심 공연에도 등록됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "201", description = "공연 알림을 설정했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PerformanceAlarmResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "SHOW201_2",
                                      "message": "공연 알림을 설정했어요.",
                                      "result": {
                                        "performanceId": 1,
                                        "isAlarmSet": true
                                      },
                                      "timeStamp": "2026-07-04 03:40:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SHOW400_2", ref = "#/components/examples/SHOW400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SHOW404_1", ref = "#/components/examples/SHOW404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SHOW409_1", ref = "#/components/examples/SHOW409_1")
                    }))
    })
    ResponseEntity<SuccessResponse<PerformanceAlarmResponse>> setAlarm(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "공연 ID", example = "1") Long performanceId
    );

    @Operation(tags = "팬모드_홈", summary = "공연 알림 해제",
            description = "참여 예정 기록을 삭제합니다. 관심 공연과 이미 완료한 참여 이력은 유지합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "공연 알림을 해제했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PerformanceAlarmResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SHOW200_5",
                                      "message": "공연 알림을 해제했어요.",
                                      "result": {
                                        "performanceId": 1,
                                        "isAlarmSet": false
                                      },
                                      "timeStamp": "2026-07-04 03:50:11"
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
                    }))
    })
    ResponseEntity<SuccessResponse<PerformanceAlarmResponse>> unsetAlarm(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "공연 ID", example = "1") Long performanceId
    );
}
