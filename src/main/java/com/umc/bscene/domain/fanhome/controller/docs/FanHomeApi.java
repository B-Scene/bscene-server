package com.umc.bscene.domain.fanhome.controller.docs;

import com.umc.bscene.domain.fanhome.dto.response.DatePerformanceResponse;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse;
import com.umc.bscene.domain.fanhome.dto.response.FollowingBandNewsResponse;
import com.umc.bscene.domain.fanhome.dto.response.PerformanceCalendarResponse;
import com.umc.bscene.domain.fanhome.dto.response.RecommendedPerformanceResponse;
import com.umc.bscene.domain.fanhome.dto.response.UpcomingPerformanceResponse;
import com.umc.bscene.domain.fanhome.enums.RecommendSortType;
import com.umc.bscene.domain.fanhome.enums.UpcomingSortType;
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

import java.time.LocalDate;

public interface FanHomeApi {

    @Operation(tags = "팬모드_홈", summary = "팬모드_홈 통합 조회",
            description = "안 읽은 알림 여부, 팔로우 밴드 소식, 추천 밴드, 공연을 함께 조회합니다. 팔로우 밴드의 다가오는 공연이 있으면 UPCOMING, 없으면 RECOMMENDED입니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "팬 홈을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.FanHomeResponseEnvelope.class), examples = {
                            @ExampleObject(name = "팔로우한 밴드 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_1",
                                      "message": "팬 홈을 조회했습니다.",
                                      "result": {
                                        "hasFollowingBands": false,
                                        "hasUnreadNotification": false,
                                        "performanceType": "RECOMMENDED",
                                        "followingBandNews": [],
                                        "recommendedBands": [
                                          {
                                            "bandId": 10,
                                            "name": "wave to earth",
                                            "genre": "인디",
                                            "region": "서울",
                                            "profileImageUrl": "https://cdn.bscene.com/band/wave.png"
                                          }
                                        ],
                                        "performances": [
                                          {
                                            "performanceId": 1,
                                            "title": "WAVY 단독 공연",
                                            "venue": "홍대 롤링홀",
                                            "performanceDate": "2026-05-17",
                                            "startTime": "18:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/perf/wavy.png"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-04 03:30:11"
                                    }
                                    """),
                            @ExampleObject(name = "팔로우 밴드의 다가오는 공연 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_1",
                                      "message": "팬 홈을 조회했습니다.",
                                      "result": {
                                        "hasFollowingBands": true,
                                        "hasUnreadNotification": false,
                                        "performanceType": "RECOMMENDED",
                                        "followingBandNews": [
                                          {
                                            "bandId": 5,
                                            "bandName": "WAVY",
                                            "bandProfileImageUrl": "https://cdn.bscene.com/band/wavy.png",
                                            "genre": "인디",
                                            "region": "서울",
                                            "postId": 100,
                                            "type": "PHOTO",
                                            "imageUrl": "https://cdn.bscene.com/post/wavy_live.png",
                                            "title": "홍대 롤링홀 라이브",
                                            "description": "다음주 홍대 롤링홀에서 라이브 공연이 예정되어있어요!",
                                            "tags": [
                                              "홍대",
                                              "인디락",
                                              "라이브"
                                            ],
                                            "createdAt": "2026-07-04T01:30:00"
                                          }
                                        ],
                                        "recommendedBands": [
                                          {
                                            "bandId": 10,
                                            "name": "wave to earth",
                                            "genre": "인디",
                                            "region": "서울",
                                            "profileImageUrl": "https://cdn.bscene.com/band/wave.png"
                                          }
                                        ],
                                        "performances": [
                                          {
                                            "performanceId": 7,
                                            "title": "인디 나잇 vol.3",
                                            "venue": "상수 클럽",
                                            "performanceDate": "2026-08-02",
                                            "startTime": "19:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/perf/indienight.png"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-04 03:30:11"
                                    }
                                    """),
                            @ExampleObject(name = "팔로우 밴드의 다가오는 공연 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_1",
                                      "message": "팬 홈을 조회했습니다.",
                                      "result": {
                                        "hasFollowingBands": true,
                                        "hasUnreadNotification": true,
                                        "performanceType": "UPCOMING",
                                        "followingBandNews": [
                                          {
                                            "bandId": 5,
                                            "bandName": "WAVY",
                                            "bandProfileImageUrl": "https://cdn.bscene.com/band/wavy.png",
                                            "genre": "인디",
                                            "region": "서울",
                                            "postId": 100,
                                            "type": "PHOTO",
                                            "imageUrl": "https://cdn.bscene.com/post/wavy_live.png",
                                            "title": "홍대 롤링홀 라이브",
                                            "description": "다음주 홍대 롤링홀에서 라이브 공연이 예정되어있어요!",
                                            "tags": [
                                              "홍대",
                                              "인디락",
                                              "라이브"
                                            ],
                                            "createdAt": "2026-07-04T01:30:00"
                                          },
                                          {
                                            "bandId": 6,
                                            "bandName": "DAYBREAK",
                                            "bandProfileImageUrl": "https://cdn.bscene.com/band/daybreak.png",
                                            "genre": "하드록",
                                            "region": "서울",
                                            "postId": 101,
                                            "type": "TEXT",
                                            "imageUrl": null,
                                            "title": "신곡 발매 소식",
                                            "description": "다음주 신곡 발표 기념 라이브가 예정되어있어요! 많은 기대 부탁드립니다.",
                                            "tags": [
                                              "신곡",
                                              "라이브",
                                              "예매"
                                            ],
                                            "createdAt": "2026-07-04T00:10:00"
                                          }
                                        ],
                                        "recommendedBands": [
                                          {
                                            "bandId": 10,
                                            "name": "wave to earth",
                                            "genre": "인디",
                                            "region": "서울",
                                            "profileImageUrl": "https://cdn.bscene.com/band/wave.png"
                                          }
                                        ],
                                        "performances": [
                                          {
                                            "performanceId": 1,
                                            "title": "WAVY 단독 공연",
                                            "venue": "홍대 롤링홀",
                                            "performanceDate": "2026-05-17",
                                            "startTime": "18:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/perf/wavy.png"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-04 03:30:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<FanHomeResponse>> getFanHome(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "팬모드_홈", summary = "팔로우한 밴드 소식 전체 조회",
            description = "팔로우한 밴드 소식을 최신순으로 조회합니다. 사진은 mediaUrls 배열로, 영상은 썸네일로 제공됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "팔로우한 밴드 소식을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.FollowingBandNewsResponseEnvelope.class), examples = {
                            @ExampleObject(name = "소식 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_2",
                                      "message": "팔로우한 밴드 소식을 조회했습니다.",
                                      "result": {
                                        "items": [
                                          {
                                            "bandId": 5,
                                            "bandName": "WAVY",
                                            "bandProfileImageUrl": "https://cdn.bscene.com/band/wavy.png",
                                            "genre": "하드록",
                                            "region": "서울",
                                            "postId": 100,
                                            "type": "PHOTO",
                                            "mediaUrls": [
                                              "https://cdn.bscene.com/post/wavy_live_1.png",
                                              "https://cdn.bscene.com/post/wavy_live_2.png"
                                            ],
                                            "title": "홍대 롤링홀 라이브",
                                            "description": "다음주 홍대 롤링홀에서 라이브 공연이 예정되어있어요!",
                                            "tags": [
                                              "홍대",
                                              "인디락",
                                              "라이브"
                                            ],
                                            "createdAt": "2026-07-04T01:30:00"
                                          },
                                          {
                                            "bandId": 6,
                                            "bandName": "DAYBREAK",
                                            "bandProfileImageUrl": "https://cdn.bscene.com/band/daybreak.png",
                                            "genre": "인디",
                                            "region": "경기",
                                            "postId": 95,
                                            "type": "VIDEO",
                                            "mediaUrls": [
                                              "https://cdn.bscene.com/post/daybreak_teaser_thumb.png"
                                            ],
                                            "title": "신곡 티저 공개",
                                            "description": "다음주 신곡 발표 기념 라이브가 예정되어있어요! 많은 기대 부탁드립니다.",
                                            "tags": [
                                              "신곡",
                                              "라이브",
                                              "예매"
                                            ],
                                            "createdAt": "2026-07-03T22:10:00"
                                          }
                                        ],
                                        "hasNext": true,
                                        "nextCursor": 95
                                      },
                                      "timeStamp": "2026-07-04 04:50:11"
                                    }
                                    """),
                            @ExampleObject(name = "소식 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_2",
                                      "message": "팔로우한 밴드 소식을 조회했습니다.",
                                      "result": {
                                        "items": [],
                                        "hasNext": false,
                                        "nextCursor": null
                                      },
                                      "timeStamp": "2026-07-04 04:50:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<FollowingBandNewsResponse>> getFollowingBandNews(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지에서는 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본값은 10이며 1~30 범위로 보정합니다.") int size
    );

    @Operation(tags = "팬모드_홈", summary = "팔로우한 밴드 다가오는 공연 전체 조회",
            description = "팔로우한 밴드의 아직 시작하지 않은 공연을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "다가오는 공연을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.UpcomingPerformanceResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_3",
                                      "message": "다가오는 공연을 조회했습니다.",
                                      "result": {
                                        "sort": "IMMINENT",
                                        "items": [
                                          {
                                            "performanceId": 1,
                                            "title": "WAVY 단독 공연",
                                            "venue": "홍대 롤링홀",
                                            "performanceDate": "2026-05-17",
                                            "startTime": "18:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/performance/wavy.png",
                                            "isInterested": true
                                          }
                                        ],
                                        "page": 0,
                                        "hasNext": true
                                      },
                                      "timeStamp": "2026-07-12 04:50:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<UpcomingPerformanceResponse>> getUpcomingPerformances(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "임박순(IMMINENT, 기본), 최신순(LATEST) 또는 인기순(POPULAR)") UpcomingSortType sort,
            @Parameter(description = "0부터 시작하는 페이지 번호. 기본값은 0이며 음수는 0으로 보정합니다.") int page,
            @Parameter(description = "페이지 크기. 기본값은 10이며 1~30 범위로 보정합니다.") int size
    );

    @Operation(tags = "팬모드_홈", summary = "추천 공연 전체 조회",
            description = "팬 홈 공연 섹션이 RECOMMENDED일 때 사용합니다. 전체 밴드의 아직 시작하지 않은 활성 공연을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "추천 공연 목록을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.RecommendedPerformanceResponseEnvelope.class), examples = {
                            @ExampleObject(name = "인기순", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_6",
                                      "message": "추천 공연 목록을 조회했습니다.",
                                      "result": {
                                        "sort": "POPULAR",
                                        "items": [
                                          {
                                            "performanceId": 12,
                                            "title": "인디 나잇 vol.3",
                                            "venue": "상수 클럽",
                                            "performanceDate": "2026-08-21",
                                            "startTime": "19:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/perf/indienight.png",
                                            "isInterested": true
                                          },
                                          {
                                            "performanceId": 11,
                                            "title": "WAVY 단독 공연",
                                            "venue": "홍대 롤링홀",
                                            "performanceDate": "2026-08-14",
                                            "startTime": "15:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/perf/wavy.png",
                                            "isInterested": false
                                          }
                                        ],
                                        "page": 0,
                                        "hasNext": true
                                      },
                                      "timeStamp": "2026-08-08 15:30:11"
                                    }
                                    """),
                            @ExampleObject(name = "임박순", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_6",
                                      "message": "추천 공연 목록을 조회했습니다.",
                                      "result": {
                                        "sort": "IMMINENT",
                                        "items": [
                                          {
                                            "performanceId": 11,
                                            "title": "WAVY 단독 공연",
                                            "venue": "홍대 롤링홀",
                                            "performanceDate": "2026-08-14",
                                            "startTime": "15:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/perf/wavy.png",
                                            "isInterested": false
                                          },
                                          {
                                            "performanceId": 12,
                                            "title": "인디 나잇 vol.3",
                                            "venue": "상수 클럽",
                                            "performanceDate": "2026-08-21",
                                            "startTime": "19:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/perf/indienight.png",
                                            "isInterested": true
                                          }
                                        ],
                                        "page": 0,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-08-08 15:30:11"
                                    }
                                    """),
                            @ExampleObject(name = "추천 공연 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_6",
                                      "message": "추천 공연 목록을 조회했습니다.",
                                      "result": {
                                        "sort": "POPULAR",
                                        "items": [],
                                        "page": 0,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-08-08 15:30:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<RecommendedPerformanceResponse>> getRecommendedPerformances(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "인기순(POPULAR, 기본) 또는 임박순(IMMINENT)") RecommendSortType sort,
            @Parameter(description = "0부터 시작하는 페이지 번호. 기본값은 0이며 음수는 0으로 보정합니다.") int page,
            @Parameter(description = "페이지 크기. 기본값은 10이며 1~30 범위로 보정합니다.") int size
    );

    @Operation(tags = "팬모드_홈", summary = "다가오는 공연 달력 조회",
            description = "팔로우한 밴드의 공연이 있는 날짜를 조회합니다. 지난 공연도 포함됩니다. 연도와 월 중 하나라도 생략하면 서버 기준 이번 달을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "공연 달력을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.PerformanceCalendarResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_4",
                                      "message": "공연 달력을 조회했습니다.",
                                      "result": {
                                        "year": 2026,
                                        "month": 6,
                                        "today": "2026-06-19",
                                        "performanceDates": [
                                          "2026-06-03",
                                          "2026-06-14",
                                          "2026-06-19",
                                          "2026-06-26"
                                        ]
                                      },
                                      "timeStamp": "2026-07-12 04:50:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PerformanceCalendarResponse>> getPerformanceCalendar(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "조회 연도. month와 함께 사용합니다.", example = "2026") Integer year,
            @Parameter(description = "조회 월 (1~12). year와 함께 사용합니다.", example = "6") Integer month
    );

    @Operation(tags = "팬모드_홈", summary = "특정 날짜 공연 목록 조회",
            description = "팔로우한 밴드의 해당 날짜 공연을 시작 시간순으로 조회합니다. 시간이 같으면 제목 가나다순입니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "해당 날짜의 공연을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.DatePerformanceResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "HOME200_5",
                                      "message": "해당 날짜의 공연을 조회했습니다.",
                                      "result": {
                                        "date": "2026-06-19",
                                        "items": [
                                          {
                                            "performanceId": 1,
                                            "title": "WAVY 단독 공연",
                                            "venue": "홍대 롤링홀",
                                            "performanceDate": "2026-06-19",
                                            "startTime": "18:00:00",
                                            "posterImageUrl": "https://cdn.bscene.com/performance/wavy.png",
                                            "isInterested": true
                                          }
                                        ],
                                        "page": 0,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-12 04:50:11"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<DatePerformanceResponse>> getPerformancesByDate(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd). 생략하면 서버 기준으로 오늘을 조회합니다.", example = "2026-06-19") LocalDate date,
            @Parameter(description = "0부터 시작하는 페이지 번호. 기본값은 0이며 음수는 0으로 보정합니다.") int page,
            @Parameter(description = "페이지 크기. 기본값은 10이며 1~30 범위로 보정합니다.") int size
    );
}
