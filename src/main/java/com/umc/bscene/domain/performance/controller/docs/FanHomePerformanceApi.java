package com.umc.bscene.domain.performance.controller.docs;

import com.umc.bscene.domain.performance.dto.response.PerformanceDetailResponse;
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

public interface FanHomePerformanceApi {

    @Operation(tags = "팬모드_홈", summary = "공연 상세페이지 조회",
            description = "공연 정보, 소개, 캐스팅, 내 관심 및 참여 상태를 조회합니다. participationStatus는 null, SCHEDULED 또는 COMPLETED입니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "공연 상세페이지를 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PerformanceDetailResponseEnvelope.class), examples = {
                            @ExampleObject(name = "알림 설정됨", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SHOW200_10",
                                      "message": "공연 상세페이지를 조회했습니다.",
                                      "result": {
                                        "performanceId": 1,
                                        "title": "WAVY 단독 공연",
                                        "genre": "인디",
                                        "performanceDate": "2026-04-12",
                                        "startTime": "19:00:00",
                                        "region": "서울",
                                        "venue": "홍대 롤링홀",
                                        "description": "WAVY의 새로운 시작을 함께해요.\\n따뜻한 사운드와 진심 어린 음악으로 여러분을 초대합니다.",
                                        "ticketPrice": "66,000원",
                                        "ticketLink": "https://ticket.example.com/wavy",
                                        "posterImageUrl": "https://cdn.bscene.com/performance/wavy.png",
                                        "ageRating": "AGE_15",
                                        "tags": [
                                          "WAVY",
                                          "단독공연",
                                          "라이브"
                                        ],
                                        "interestCount": 42,
                                        "isInterested": true,
                                        "participationStatus": "SCHEDULED",
                                        "casting": [
                                          {
                                            "bandId": 1,
                                            "name": "WAVY",
                                            "genre": "인디",
                                            "region": "서울",
                                            "profileImageUrl": "https://cdn.bscene.com/band/wavy.png"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-22 00:20:11"
                                    }
                                    """),
                            @ExampleObject(name = "알림 미설정", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SHOW200_10",
                                      "message": "공연 상세페이지를 조회했습니다.",
                                      "result": {
                                        "performanceId": 2,
                                        "title": "여름밤 어쿠스틱 공연",
                                        "genre": "포크록",
                                        "performanceDate": "2026-08-19",
                                        "startTime": "18:30:00",
                                        "region": "서울",
                                        "venue": "롤링홀",
                                        "description": "여름밤을 수놓는 어쿠스틱 라이브.",
                                        "ticketPrice": "30,000원",
                                        "ticketLink": null,
                                        "posterImageUrl": null,
                                        "ageRating": "ALL",
                                        "tags": [],
                                        "interestCount": 0,
                                        "isInterested": false,
                                        "participationStatus": null,
                                        "casting": [
                                          {
                                            "bandId": 2,
                                            "name": "달빛창가",
                                            "genre": "포크록",
                                            "region": "서울",
                                            "profileImageUrl": null
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-22 00:20:11"
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
    ResponseEntity<SuccessResponse<PerformanceDetailResponse>> getPerformanceDetailPage(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "공연 ID", example = "1") Long performanceId
    );
}
