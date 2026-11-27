package com.umc.bscene.domain.search.controller.docs;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse;
import com.umc.bscene.domain.search.dto.response.RecentSearchListResponse;
import com.umc.bscene.domain.search.enums.SearchSortType;
import com.umc.bscene.domain.search.enums.SearchType;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.ExploreApiResponseSchemas;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

public interface ExploreSearchApi {

    @Operation(tags = "팬모드_탐색", summary = "탐색 통합 검색",
            description = "ALL은 밴드·공연·게시물을 각각 최대 3개 반환합니다. 단일 타입은 선택한 섹션을 반환하며 다음 페이지가 있으면 커서를 제공합니다. 검색에 성공한 첫 페이지 요청의 검색어를 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "검색 결과를 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.ExploreSearchResponseEnvelope.class), examples = {
                            @ExampleObject(name = "통합 검색", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SEARCH200_1",
                                      "message": "검색 결과를 조회했습니다.",
                                      "result": {
                                        "totalCount": 6,
                                        "type": "ALL",
                                        "bands": {
                                          "totalCount": 1,
                                          "items": [
                                            {
                                              "bandId": 3,
                                              "name": "블루문",
                                              "genre": "HARD_ROCK",
                                              "region": "SEOUL",
                                              "description": "홍대에서 활동하는 4인조 록밴드입니다.",
                                              "profileImageUrl": "https://cdn.bscene.com/bands/3/profile.jpg",
                                              "isFollowing": true
                                            }
                                          ]
                                        },
                                        "performances": {
                                          "totalCount": 2,
                                          "items": [
                                            {
                                              "performanceId": 17,
                                              "title": "블루문 단독 공연 : 만월",
                                              "bandName": "블루문",
                                              "venue": "홍대 롤링홀",
                                              "performanceDate": "2026-08-15",
                                              "startTime": "19:00:00",
                                              "genre": "HARD_ROCK",
                                              "region": "SEOUL",
                                              "posterImageUrl": "https://cdn.bscene.com/performances/17/poster.jpg"
                                            },
                                            {
                                              "performanceId": 21,
                                              "title": "블루문 X 새벽감성 합동 공연",
                                              "bandName": "블루문",
                                              "venue": "클럽 FF",
                                              "performanceDate": "2026-09-02",
                                              "startTime": "18:30:00",
                                              "genre": "HARD_ROCK",
                                              "region": "SEOUL",
                                              "posterImageUrl": "https://cdn.bscene.com/performances/21/poster.jpg"
                                            }
                                          ]
                                        },
                                        "posts": {
                                          "totalCount": 3,
                                          "items": [
                                            {
                                              "postId": 42,
                                              "postType": "VIDEO",
                                              "title": "블루문 신곡 라이브 클립",
                                              "bandName": "블루문",
                                              "bandProfileImageUrl": "https://cdn.bscene.com/bands/3/profile.jpg",
                                              "description": "8월 단독 공연에서 처음 공개한 신곡입니다.",
                                              "tags": [
                                                "라이브",
                                                "신곡"
                                              ],
                                              "thumbnailUrl": "https://cdn.bscene.com/posts/42/thumbnail.jpg",
                                              "uploadedAt": "2026-07-18T21:03:12"
                                            },
                                            {
                                              "postId": 38,
                                              "postType": "PHOTO",
                                              "title": "합주실 비하인드",
                                              "bandName": "블루문",
                                              "bandProfileImageUrl": "https://cdn.bscene.com/bands/3/profile.jpg",
                                              "description": "공연 준비 중인 멤버들",
                                              "tags": [
                                                "비하인드"
                                              ],
                                              "thumbnailUrl": "https://cdn.bscene.com/posts/38/media/1.jpg",
                                              "uploadedAt": "2026-07-15T14:20:45"
                                            },
                                            {
                                              "postId": 35,
                                              "postType": "TEXT",
                                              "title": "공연 후기 감사 인사",
                                              "bandName": "블루문",
                                              "bandProfileImageUrl": "https://cdn.bscene.com/bands/3/profile.jpg",
                                              "description": "와주신 모든 분들 감사합니다!",
                                              "tags": [],
                                              "thumbnailUrl": null,
                                              "uploadedAt": "2026-07-10T09:12:30"
                                            }
                                          ]
                                        },
                                        "nextCursor": null,
                                        "hasNext": null
                                      },
                                      "timeStamp": "2026-07-20 14:22:35"
                                    }
                                    """),
                            @ExampleObject(name = "밴드 검색", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SEARCH200_1",
                                      "message": "검색 결과를 조회했습니다.",
                                      "result": {
                                        "totalCount": 14,
                                        "type": "BAND",
                                        "bands": {
                                          "totalCount": 14,
                                          "items": [
                                            {
                                              "bandId": 3,
                                              "name": "블루문",
                                              "genre": "HARD_ROCK",
                                              "region": "SEOUL",
                                              "description": "홍대에서 활동하는 4인조 록밴드입니다.",
                                              "profileImageUrl": "https://cdn.bscene.com/bands/3/profile.jpg",
                                              "isFollowing": true
                                            },
                                            {
                                              "bandId": 8,
                                              "name": "블루문라이징",
                                              "genre": "INDIE",
                                              "region": "BUSAN",
                                              "description": "부산 기반 인디팝 밴드",
                                              "profileImageUrl": "https://cdn.bscene.com/bands/8/profile.jpg",
                                              "isFollowing": false
                                            }
                                          ]
                                        },
                                        "performances": null,
                                        "posts": null,
                                        "nextCursor": "UHwxNXwxMi4zNHw3",
                                        "hasNext": true
                                      },
                                      "timeStamp": "2026-07-20 14:22:35"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SEARCH400_1", ref = "#/components/examples/SEARCH400_1"),
                            @ExampleObject(name = "SEARCH400_2", ref = "#/components/examples/SEARCH400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "503", description = "검색 서비스 일시 중단", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SEARCH503_1", ref = "#/components/examples/SEARCH503_1")
                    }))
    })
    ResponseEntity<SuccessResponse<ExploreSearchResponse>> search(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "필수 검색어", example = "블루문") String keyword,
            @Parameter(description = "검색 대상. 기본값은 ALL입니다.") SearchType type,
            @Parameter(description = "정렬 기준. 기본값은 ACCURACY입니다.") SearchSortType sort,
            @Parameter(description = "장르 코드", example = "INDIE") Genre genre,
            @Parameter(description = "지역 코드", example = "SEOUL") Region region,
            @Parameter(description = "더미 밴드와 관련 콘텐츠 포함 여부. 기본값은 true입니다.") boolean withDummy,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지는 생략합니다.") String cursor,
            @Parameter(description = "페이지 크기. 기본값은 10이며 1~30 범위로 제한됩니다.") int size
    );

    @Operation(tags = "팬모드_탐색", summary = "최근 검색어 조회",
            description = "최근 검색어를 최신순으로 최대 10개 조회합니다. 검색 실행 시 자동 저장됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "최근 검색어를 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.RecentSearchListResponseEnvelope.class), examples = {
                            @ExampleObject(name = "최근 검색어 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SEARCH200_2",
                                      "message": "최근 검색어를 조회했습니다.",
                                      "result": {
                                        "recentSearches": [
                                          {
                                            "recentSearchId": 12,
                                            "keyword": "블루문"
                                          },
                                          {
                                            "recentSearchId": 9,
                                            "keyword": "홍대 공연"
                                          },
                                          {
                                            "recentSearchId": 7,
                                            "keyword": "인디락"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-20 14:22:35"
                                    }
                                    """),
                            @ExampleObject(name = "최근 검색어 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SEARCH200_2",
                                      "message": "최근 검색어를 조회했습니다.",
                                      "result": {
                                        "recentSearches": []
                                      },
                                      "timeStamp": "2026-07-20 14:22:35"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<RecentSearchListResponse>> getRecentSearches(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "팬모드_탐색", summary = "최근 검색어 개별 삭제",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "최근 검색어를 삭제했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SEARCH200_3",
                                      "message": "최근 검색어를 삭제했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-20 14:22:35"
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
                            @ExampleObject(name = "SEARCH404_1", ref = "#/components/examples/SEARCH404_1")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> deleteRecentSearch(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "최근 검색어 ID", example = "12") Long recentSearchId
    );

    @Operation(tags = "팬모드_탐색", summary = "최근 검색어 전체 삭제",
            description = "로그인한 사용자의 최근 검색어를 모두 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "최근 검색어를 전체 삭제했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SEARCH200_4",
                                      "message": "최근 검색어를 전체 삭제했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-20 14:22:35"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> deleteAllRecentSearches(
            @Parameter(hidden = true) AuthMember authMember
    );
}
