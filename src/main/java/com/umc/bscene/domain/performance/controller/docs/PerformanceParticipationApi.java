package com.umc.bscene.domain.performance.controller.docs;

import com.umc.bscene.domain.performance.dto.response.PendingParticipationResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceParticipationDeclineResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceParticipationResponse;
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

public interface PerformanceParticipationApi {

    @Operation(tags = "팬모드_홈", summary = "참여 확인 대기 공연 목록 조회",
            description = "알림을 설정한 공연 중 이미 시작했고 아직 참여 여부를 확인하지 않은 공연을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "참여 확인 대기 공연 목록을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PendingParticipationResponseEnvelope.class), examples = {
                            @ExampleObject(name = "확인 대기 공연 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SHOW200_8",
                                      "message": "참여 확인 대기 공연 목록을 조회했습니다.",
                                      "result": {
                                        "items": [
                                          {
                                            "performanceId": 1,
                                            "title": "홍대 라이브 나이트",
                                            "venue": "클럽 FF",
                                            "performanceDate": "2026-07-18",
                                            "startTime": "19:00:00",
                                            "posterImageUrl": "https://s3.amazonaws.com/bscene/poster1.jpg"
                                          },
                                          {
                                            "performanceId": 2,
                                            "title": "여름밤 어쿠스틱 공연",
                                            "venue": "롤링홀",
                                            "performanceDate": "2026-07-19",
                                            "startTime": "18:30:00",
                                            "posterImageUrl": "https://s3.amazonaws.com/bscene/poster2.jpg"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-20 21:15:32"
                                    }
                                    """),
                            @ExampleObject(name = "확인 대기 공연 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SHOW200_8",
                                      "message": "참여 확인 대기 공연 목록을 조회했습니다.",
                                      "result": {
                                        "items": []
                                      },
                                      "timeStamp": "2026-07-20 21:15:32"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PendingParticipationResponse>> getPendingParticipations(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "팬모드_홈", summary = "공연 참여 완료",
            description = "알림을 설정한 공연이 시작한 후 참여를 완료 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "공연 참여를 완료했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PerformanceParticipationResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SHOW200_7",
                                      "message": "공연 참여를 완료했어요.",
                                      "result": {
                                        "performanceId": 1,
                                        "status": "COMPLETED"
                                      },
                                      "timeStamp": "2026-07-13 04:50:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SHOW400_3", ref = "#/components/examples/SHOW400_3"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SHOW404_2", ref = "#/components/examples/SHOW404_2"),
                            @ExampleObject(name = "SHOW404_1", ref = "#/components/examples/SHOW404_1")
                    }))
    })
    ResponseEntity<SuccessResponse<PerformanceParticipationResponse>> completeParticipation(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "공연 ID", example = "1") Long performanceId
    );

    @Operation(tags = "팬모드_홈", summary = "공연 불참",
            description = "참여 예정 기록을 삭제합니다. 관심 공연은 유지합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "공연 불참 처리했어요.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PerformanceParticipationDeclineResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SHOW200_9",
                                      "message": "공연 불참 처리했어요.",
                                      "result": {
                                        "performanceId": 1
                                      },
                                      "timeStamp": "2026-07-20 21:15:32"
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
                            @ExampleObject(name = "SHOW404_2", ref = "#/components/examples/SHOW404_2")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SHOW409_3", ref = "#/components/examples/SHOW409_3")
                    }))
    })
    ResponseEntity<SuccessResponse<PerformanceParticipationDeclineResponse>> declineParticipation(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "공연 ID", example = "1") Long performanceId
    );
}
