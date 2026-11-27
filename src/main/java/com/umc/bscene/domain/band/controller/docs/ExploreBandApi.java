package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.response.BandDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
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

public interface ExploreBandApi {

    @Operation(tags = "팬모드_탐색", summary = "추천 밴드 조회",
            description = "개인화 추천 밴드 목록을 조회합니다. 다음 페이지는 응답의 nextCursor를 사용합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "추천 밴드 목록을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.BandRecommendResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "BAND200_10",
                                      "message": "추천 밴드 목록을 조회했습니다.",
                                      "result": {
                                        "bands": [
                                          {
                                            "bandId": 300,
                                            "name": "세렝게티",
                                            "genre": "INDIE",
                                            "region": "SEOUL",
                                            "profileImageUrl": null,
                                            "description": "세렝게티는 아프로 소울 펑크를 구사하는 남성 3인조 록 밴드이다.",
                                            "score": 7.118250000000001,
                                            "reason": "선호 장르 일치"
                                          },
                                          {
                                            "bandId": 211,
                                            "name": "얄개들",
                                            "genre": "INDIE",
                                            "region": "SEOUL",
                                            "profileImageUrl": null,
                                            "description": "얄개들은 직설적인 가사와 펑크 에너지를 내세우는 인디 록 밴드이다.",
                                            "score": 6.97325,
                                            "reason": "선호 장르 일치"
                                          }
                                        ],
                                        "hasNext": true,
                                        "nextCursor": 2,
                                        "fallback": false
                                      },
                                      "timeStamp": "2026-07-20 04:38:16"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandRecommendResponse>> getRecommendedBands(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지는 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본값 및 상한은 10입니다.") Integer size,
            @Parameter(description = "더미 밴드와 관련 콘텐츠 포함 여부. 기본값은 true입니다.") boolean withDummy
    );

    @Operation(tags = "팬모드_탐색", summary = "밴드 프로필 조회",
            description = "밴드 기본 정보, 팔로우 상태, 라이브 진행 여부를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "밴드 상세 정보를 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ExploreApiResponseSchemas.BandDetailResponseEnvelope.class), examples = {
                            @ExampleObject(name = "라이브 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "BAND200_12",
                                      "message": "밴드 상세 정보를 조회했습니다.",
                                      "result": {
                                        "bandId": 1,
                                        "name": "WAVY",
                                        "genre": "INDIE",
                                        "region": "SEOUL",
                                        "profileImageUrl": "https://cdn.bscene.com/band-profile.jpg",
                                        "description": "몽환적인 사운드와 감각적인 스타일로 주목받는 3인조 밴드",
                                        "followerCount": 560,
                                        "isFollowing": false,
                                        "isLive": false,
                                        "liveId": null
                                      },
                                      "timeStamp": "2026-07-21 23:40:11"
                                    }
                                    """),
                            @ExampleObject(name = "라이브 중", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "BAND200_12",
                                      "message": "밴드 상세 정보를 조회했습니다.",
                                      "result": {
                                        "bandId": 1,
                                        "name": "WAVY",
                                        "genre": "INDIE",
                                        "region": "SEOUL",
                                        "profileImageUrl": "https://cdn.bscene.com/band-profile.jpg",
                                        "description": "몽환적인 사운드와 감각적인 스타일로 주목받는 3인조 밴드",
                                        "followerCount": 560,
                                        "isFollowing": true,
                                        "isLive": true,
                                        "liveId": 12
                                      },
                                      "timeStamp": "2026-07-21 23:40:11"
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
                            @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                    }))
    })
    ResponseEntity<SuccessResponse<BandDetailResponse>> getBandDetail(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "밴드 ID", example = "1") Long bandId
    );
}
