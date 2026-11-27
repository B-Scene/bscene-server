package com.umc.bscene.domain.session.controller.docs;

import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationCreateRequest;
import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.request.SessionApplicationVisibilityRequest;
import com.umc.bscene.domain.session.dto.application.response.MyApplicationSubmissionListResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationSummaryResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSearchResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationVisibilityResponse;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import com.umc.bscene.global.swagger.SessionApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public interface SessionApplicationApi {

    @Operation(tags = "세션", summary = "세션 찾기 조회",
            description = "지역·장르·파트·실력대·검색어가 모두 없으면 내 기본 지원서와 활동 지역 및 장르가 모두 일치하는 세션을 먼저 보여줍니다. 한 항목만 일치하는 세션이 그다음이며 같은 순위는 지원서 ID 내림차순입니다. 필터를 적용하거나 기본 지원서가 없으면 지원서 ID 내림차순으로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 찾기 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionApplicationSearchResponseEnvelope.class), examples = {
                            @ExampleObject(name = "데이터 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_8",
                                      "message": "세션 찾기 조회에 성공했습니다.",
                                      "result": {
                                        "content": [
                                          {
                                            "sessionApplicationId": 18,
                                            "userId": 2,
                                            "nickname": "정하람",
                                            "profileImageUrl": "https://example.com/session-profile.jpg",
                                            "skillLevel": "중급",
                                            "part": "드럼",
                                            "genre": "인디",
                                            "region": "서울",
                                            "title": "녹음 및 코러스 가능",
                                            "oneLineIntro": "잔잔한 톤의 보컬과 가이드 녹음이 가능합니다."
                                          },
                                          {
                                            "sessionApplicationId": 15,
                                            "userId": 3,
                                            "nickname": "이지수",
                                            "profileImageUrl": null,
                                            "skillLevel": "상급",
                                            "part": "보컬",
                                            "genre": "팝",
                                            "region": "서울",
                                            "title": "편곡 및 세션 연주",
                                            "oneLineIntro": "공연과 녹음 세션 경험이 있습니다."
                                          }
                                        ],
                                        "size": 2,
                                        "nextCursor": 15,
                                        "hasNext": true
                                      },
                                      "timeStamp": "2026-07-11 12:00:00"
                                    }
                                    """),
                            @ExampleObject(name = "데이터 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_8",
                                      "message": "세션 찾기 조회에 성공했습니다.",
                                      "result": {
                                        "content": [],
                                        "size": 10,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-11 12:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<SessionApplicationSearchResponse> searchSessionApplications(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "활동 지역", example = "서울") String region,
            @Parameter(description = "실력대", example = "중급") SkillLevel skillLevel,
            @Parameter(description = "파트", example = "드럼") Part part,
            @Parameter(description = "장르", example = "인디") String genre,
            @Parameter(description = "검색어", example = "드럼") String keyword,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지에서는 생략") Long cursorId,
            @Parameter(description = "페이지 크기") Integer size
    );

    @Operation(tags = "세션", summary = "세션 지원서 상세 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 지원서 상세 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionApplicationDetailResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_9",
                                      "message": "세션 지원서 상세 조회에 성공했습니다.",
                                      "result": {
                                        "sessionApplicationId": 53,
                                        "title": "인디밴드 드럼 지원서",
                                        "purpose": "기본",
                                        "userId": 1,
                                        "nickname": "홍길동",
                                        "profileImageUrl": "https://example.com/session-profile.jpg",
                                        "isPublic": true,
                                        "oneLineIntro": "선명한 리듬과 안정적인 합주",
                                        "intro": "안녕하세요. 드럼 세션으로 활동 중인 정하람입니다. 정기 합주와 소규모 라이브 경험을 바탕으로 밴드의 전체 사운드를 해치지 않으면서 안정적으로 리듬을 잡는 편입니다. 인디팝과 록 장르를 주로 연주하며 공연과 음원 작업 일정에 꾸준히 참여할 수 있습니다.",
                                        "part": "드럼",
                                        "skillLevel": "중급",
                                        "genre": "인디",
                                        "region": "서울",
                                        "availableActivities": [
                                          "정기 합주",
                                          "라이브 공연",
                                          "앨범 및 음원 작업"
                                        ],
                                        "careers": [
                                          {
                                            "sessionApplicationCareerId": 95,
                                            "name": "B밴드 드러머 활동",
                                            "period": "2024.06 ~ 현재",
                                            "description": "정기 합주, 소규모 라이브 8회, 공연 세트리스트 구성과 리듬 편곡에 참여했습니다."
                                          },
                                          {
                                            "sessionApplicationCareerId": 96,
                                            "name": "홍대 라이브클럽 드럼 세션",
                                            "period": "2025.09",
                                            "description": "A밴드 단독 공연의 드럼 세션으로 참여했습니다."
                                          }
                                        ],
                                        "portfolioLinks": [
                                          {
                                            "sessionApplicationLinkId": 131,
                                            "url": "https://youtu.be/7qvrRWMXY-8?si=QnSZnEevCxlcSDyP",
                                            "title": "LUCY(루시) - 개화 (Flowering) [가사/Lyrics]",
                                            "thumbnailUrl": "https://img.youtube.com/vi/7qvrRWMXY-8/hqdefault.jpg",
                                            "mediaType": "YOUTUBE"
                                          },
                                          {
                                            "sessionApplicationLinkId": 132,
                                            "url": "https://soundcloud.com/example/drum-performance",
                                            "title": "SoundCloud",
                                            "thumbnailUrl": null,
                                            "mediaType": "SOUNDCLOUD"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-22 18:51:54"
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
                            @ExampleObject(name = "SESSION404_1", ref = "#/components/examples/SESSION404_1")
                    }))
    })
    SuccessResponse<SessionApplicationDetailResponse> getSessionApplicationDetail(
            @Parameter(description = "세션 지원서 ID", example = "1") Long sessionApplicationId
    );

    @Operation(tags = "세션", summary = "기본 지원서 공개 설정 변경",
            description = "기본 지원서의 공개 여부를 변경합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SessionApplicationVisibilityRequest.class), examples = {
                            @ExampleObject(name = "공개", value = """
                                    {
                                      "isPublic": true
                                    }
                                    """),
                            @ExampleObject(name = "비공개", value = """
                                    {
                                      "isPublic": false
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "기본 지원서 공개 설정 변경에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionApplicationVisibilityResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_6",
                                      "message": "기본 지원서 공개 설정 변경에 성공했습니다.",
                                      "result": {
                                        "sessionApplicationId": 1,
                                        "isPublic": false
                                      },
                                      "timeStamp": "2026-07-11 12:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION400_2", ref = "#/components/examples/SESSION400_2"),
                            @ExampleObject(name = "SESSION400_3", ref = "#/components/examples/SESSION400_3"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION404_1", ref = "#/components/examples/SESSION404_1")
                    }))
    })
    SuccessResponse<SessionApplicationVisibilityResponse> updateVisibility(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "세션 지원서 ID", example = "1") Long sessionApplicationId,
            SessionApplicationVisibilityRequest request
    );

    @Operation(tags = "세션", summary = "내 지원서 요약 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "내 세션 지원서 요약 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.MySessionApplicationSummaryResponseEnvelope.class), examples = {
                            @ExampleObject(name = "기본 지원서 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_7",
                                      "message": "내 세션 지원서 요약 조회에 성공했습니다.",
                                      "result": {
                                        "hasDefaultApplication": true,
                                        "sessionApplicationId": 8,
                                        "nickname": "홍길동",
                                        "profileImageUrl": null,
                                        "skillLevel": "중급",
                                        "part": "드럼",
                                        "genre": "인디",
                                        "region": "서울",
                                        "applicationCount": 2,
                                        "submissionCount": 4,
                                        "inProgressCount": 1,
                                        "applications": [
                                          {
                                            "sessionApplicationId": 8,
                                            "displayDate": "2026-07-10T15:30:00",
                                            "isModified": true,
                                            "isPublic": false,
                                            "purpose": "기본",
                                            "title": "드럼 세션 지원합니다"
                                          },
                                          {
                                            "sessionApplicationId": 9,
                                            "displayDate": "2026-07-11T10:00:00",
                                            "isModified": false,
                                            "purpose": "공연",
                                            "title": "공연 드럼 세션 지원합니다"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-21 18:00:00"
                                    }
                                    """),
                            @ExampleObject(name = "기본 지원서 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_7",
                                      "message": "내 세션 지원서 요약 조회에 성공했습니다.",
                                      "result": {
                                        "hasDefaultApplication": false,
                                        "sessionApplicationId": null,
                                        "nickname": "홍길동",
                                        "profileImageUrl": null,
                                        "skillLevel": null,
                                        "part": null,
                                        "genre": null,
                                        "region": null,
                                        "applicationCount": 1,
                                        "submissionCount": 2,
                                        "inProgressCount": 1,
                                        "applications": [
                                          {
                                            "sessionApplicationId": 9,
                                            "displayDate": "2026-07-11T10:00:00",
                                            "isModified": false,
                                            "purpose": "공연",
                                            "title": "공연 드럼 세션 지원합니다"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-21 18:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<MySessionApplicationSummaryResponse> getMySessionApplicationSummary(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "세션", summary = "내 지원서 생성",
            description = "첫 지원서는 기본 용도로 생성해야 하며 기본 지원서는 한 개만 등록할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MySessionApplicationCreateRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "purpose": "기본",
                                      "title": "인디밴드 드럼 지원서",
                                      "oneLineIntro": "선명한 리듬과 안정적인 합주",
                                      "intro": "안녕하세요. 드럼 세션으로 활동 중인 정하람입니다. 정기 합주와 소규모 라이브 경험을 바탕으로 밴드의 전체 사운드를 해치지 않으면서 안정적으로 리듬을 잡는 편입니다. 인디팝과 록 장르를 주로 연주하며 공연과 음원 작업 일정에 꾸준히 참여할 수 있습니다.",
                                      "part": "드럼",
                                      "skillLevel": "중급",
                                      "genre": "인디",
                                      "region": "서울",
                                      "availableActivities": [
                                        "정기 합주",
                                        "라이브 공연",
                                        "앨범 및 음원 작업"
                                      ],
                                      "careers": [
                                        {
                                          "name": "B밴드 드러머 활동",
                                          "period": "2024.06 ~ 현재",
                                          "description": "정기 합주, 소규모 라이브 8회, 공연 세트리스트 구성과 리듬 편곡에 참여했습니다."
                                        },
                                        {
                                          "name": "홍대 라이브클럽 드럼 세션",
                                          "period": "2025.09",
                                          "description": "A밴드 단독 공연의 드럼 세션으로 참여했습니다."
                                        }
                                      ],
                                      "portfolioLinks": [
                                        {
                                          "url": "https://youtu.be/7qvrRWMXY-8?si=QnSZnEevCxlcSDyP"
                                        },
                                        {
                                          "url": "https://soundcloud.com/example/drum-performance"
                                        }
                                      ]
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "201", description = "지원서 생성에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.MySessionApplicationResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "SESSION201_1",
                                      "message": "지원서 생성에 성공했습니다.",
                                      "result": {
                                        "hasApplication": true,
                                        "sessionApplicationId": 54,
                                        "userId": 1,
                                        "nickname": "홍길동",
                                        "title": "인디밴드 드럼 지원서",
                                        "purpose": "기본",
                                        "oneLineIntro": "선명한 리듬과 안정적인 합주",
                                        "profileImageUrl": null,
                                        "part": "드럼",
                                        "skillLevel": "중급",
                                        "genre": "인디",
                                        "region": "서울",
                                        "intro": "안녕하세요. 드럼 세션으로 활동 중인 정하람입니다. 정기 합주와 소규모 라이브 경험을 바탕으로 밴드의 전체 사운드를 해치지 않으면서 안정적으로 리듬을 잡는 편입니다. 인디팝과 록 장르를 주로 연주하며 공연과 음원 작업 일정에 꾸준히 참여할 수 있습니다.",
                                        "portfolioLinks": [
                                          {
                                            "sessionApplicationLinkId": 123,
                                            "url": "https://youtu.be/7qvrRWMXY-8?si=QnSZnEevCxlcSDyP",
                                            "title": null,
                                            "thumbnailUrl": null,
                                            "mediaType": null
                                          },
                                          {
                                            "sessionApplicationLinkId": 124,
                                            "url": "https://soundcloud.com/example/drum-performance",
                                            "title": null,
                                            "thumbnailUrl": null,
                                            "mediaType": null
                                          }
                                        ],
                                        "availableActivities": [
                                          "정기 합주",
                                          "라이브 공연",
                                          "앨범 및 음원 작업"
                                        ],
                                        "careers": [
                                          {
                                            "sessionApplicationCareerId": 87,
                                            "name": "B밴드 드러머 활동",
                                            "period": "2024.06 ~ 현재",
                                            "description": "정기 합주, 소규모 라이브 8회, 공연 세트리스트 구성과 리듬 편곡에 참여했습니다."
                                          },
                                          {
                                            "sessionApplicationCareerId": 88,
                                            "name": "홍대 라이브클럽 드럼 세션",
                                            "period": "2025.09",
                                            "description": "A밴드 단독 공연의 드럼 세션으로 참여했습니다."
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-22 18:31:16"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION400_2", ref = "#/components/examples/SESSION400_2"),
                            @ExampleObject(name = "SESSION400_7", ref = "#/components/examples/SESSION400_7"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION409_2", ref = "#/components/examples/SESSION409_2")
                    }))
    })
    SuccessResponse<MySessionApplicationResponse> createSessionApplication(
            @Parameter(hidden = true) AuthMember authMember,
            MySessionApplicationCreateRequest request
    );

    @Operation(tags = "세션", summary = "내 지원서 수정",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MySessionApplicationUpdateRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "purpose": "공연",
                                      "title": "인디밴드 드럼 지원서",
                                      "oneLineIntro": "선명한 리듬과 안정적인 합주",
                                      "intro": "안녕하세요. 드럼 세션으로 활동 중인 정하람입니다. 정기 합주와 소규모 라이브 경험을 바탕으로 밴드의 전체 사운드를 해치지 않으면서 안정적으로 리듬을 잡는 편입니다. 인디팝과 록 장르를 주로 연주하며 공연과 음원 작업 일정에 꾸준히 참여할 수 있습니다.",
                                      "part": "드럼",
                                      "skillLevel": "중급",
                                      "genre": "인디",
                                      "region": "서울",
                                      "availableActivities": [
                                        "정기 합주",
                                        "라이브 공연",
                                        "앨범 및 음원 작업"
                                      ],
                                      "careers": [
                                        {
                                          "name": "B밴드 드러머 활동",
                                          "period": "2024.06 ~ 현재",
                                          "description": "정기 합주, 소규모 라이브 8회, 공연 세트리스트 구성과 리듬 편곡에 참여했습니다."
                                        },
                                        {
                                          "name": "홍대 라이브클럽 드럼 세션",
                                          "period": "2025.09",
                                          "description": "A밴드 단독 공연의 드럼 세션으로 참여했습니다."
                                        }
                                      ],
                                      "portfolioLinks": [
                                        {
                                          "url": "https://youtu.be/7qvrRWMXY-8?si=QnSZnEevCxlcSDyP"
                                        },
                                        {
                                          "url": "https://soundcloud.com/example/drum-performance"
                                        }
                                      ]
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "내 지원서 저장에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.MySessionApplicationResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_4",
                                      "message": "내 지원서 저장에 성공했습니다.",
                                      "result": {
                                        "hasApplication": true,
                                        "sessionApplicationId": 53,
                                        "userId": 1,
                                        "nickname": "홍길동",
                                        "title": "인디밴드 드럼 지원서",
                                        "purpose": "공연",
                                        "oneLineIntro": "선명한 리듬과 안정적인 합주",
                                        "profileImageUrl": null,
                                        "part": "드럼",
                                        "skillLevel": "중급",
                                        "genre": "인디",
                                        "region": "서울",
                                        "intro": "안녕하세요. 드럼 세션으로 활동 중인 정하람입니다. 정기 합주와 소규모 라이브 경험을 바탕으로 밴드의 전체 사운드를 해치지 않으면서 안정적으로 리듬을 잡는 편입니다. 인디팝과 록 장르를 주로 연주하며 공연과 음원 작업 일정에 꾸준히 참여할 수 있습니다.",
                                        "portfolioLinks": [
                                          {
                                            "sessionApplicationLinkId": 131,
                                            "url": "https://youtu.be/7qvrRWMXY-8?si=QnSZnEevCxlcSDyP",
                                            "title": null,
                                            "thumbnailUrl": null,
                                            "mediaType": null
                                          },
                                          {
                                            "sessionApplicationLinkId": 132,
                                            "url": "https://soundcloud.com/example/drum-performance",
                                            "title": null,
                                            "thumbnailUrl": null,
                                            "mediaType": null
                                          }
                                        ],
                                        "availableActivities": [
                                          "정기 합주",
                                          "라이브 공연",
                                          "앨범 및 음원 작업"
                                        ],
                                        "careers": [
                                          {
                                            "sessionApplicationCareerId": 95,
                                            "name": "B밴드 드러머 활동",
                                            "period": "2024.06 ~ 현재",
                                            "description": "정기 합주, 소규모 라이브 8회, 공연 세트리스트 구성과 리듬 편곡에 참여했습니다."
                                          },
                                          {
                                            "sessionApplicationCareerId": 96,
                                            "name": "홍대 라이브클럽 드럼 세션",
                                            "period": "2025.09",
                                            "description": "A밴드 단독 공연의 드럼 세션으로 참여했습니다."
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-22 18:41:42"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION400_2", ref = "#/components/examples/SESSION400_2"),
                            @ExampleObject(name = "SESSION400_9", ref = "#/components/examples/SESSION400_9"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION404_1", ref = "#/components/examples/SESSION404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION409_2", ref = "#/components/examples/SESSION409_2")
                    }))
    })
    SuccessResponse<MySessionApplicationResponse> updateSessionApplication(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "세션 지원서 ID", example = "1") Long sessionApplicationId,
            MySessionApplicationUpdateRequest request
    );

    @Operation(tags = "세션", summary = "내 지원서 삭제",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "내 지원서 삭제에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_5",
                                      "message": "내 지원서 삭제에 성공했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-11 12:00:00"
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
                            @ExampleObject(name = "SESSION404_1", ref = "#/components/examples/SESSION404_1")
                    }))
    })
    SuccessResponse<Void> deleteSessionApplication(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "세션 지원서 ID", example = "1") Long sessionApplicationId
    );

    @Operation(tags = "세션", summary = "내 지원서 상세 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "내 지원서 상세 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.MySessionApplicationDetailResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_3",
                                      "message": "내 지원서 상세 조회에 성공했습니다.",
                                      "result": {
                                        "modifiedAt": "2026-07-22T18:41:42.008986",
                                        "profileImageUrl": "https://example.com/session-profile.jpg",
                                        "name": "홍길동",
                                        "defaultPart": "드럼",
                                        "defaultSkillLevel": "중급",
                                        "defaultRegion": "서울",
                                        "purpose": "공연",
                                        "title": "인디밴드 드럼 지원서",
                                        "oneLineIntro": "선명한 리듬과 안정적인 합주",
                                        "intro": "안녕하세요. 드럼 세션으로 활동 중인 정하람입니다. 정기 합주와 소규모 라이브 경험을 바탕으로 밴드의 전체 사운드를 해치지 않으면서 안정적으로 리듬을 잡는 편입니다. 인디팝과 록 장르를 주로 연주하며 공연과 음원 작업 일정에 꾸준히 참여할 수 있습니다.",
                                        "part": "드럼",
                                        "skillLevel": "중급",
                                        "genre": "인디",
                                        "region": "서울",
                                        "availableActivities": [
                                          "정기 합주",
                                          "라이브 공연",
                                          "앨범 및 음원 작업"
                                        ],
                                        "careers": [
                                          {
                                            "name": "B밴드 드러머 활동",
                                            "period": "2024.06 ~ 현재",
                                            "description": "정기 합주, 소규모 라이브 8회, 공연 세트리스트 구성과 리듬 편곡에 참여했습니다."
                                          },
                                          {
                                            "name": "홍대 라이브클럽 드럼 세션",
                                            "period": "2025.09",
                                            "description": "A밴드 단독 공연의 드럼 세션으로 참여했습니다."
                                          }
                                        ],
                                        "portfolioLinks": [
                                          {
                                            "url": "https://youtu.be/7qvrRWMXY-8?si=QnSZnEevCxlcSDyP",
                                            "title": "LUCY(루시) - 개화 (Flowering) [가사/Lyrics]",
                                            "thumbnailUrl": "https://img.youtube.com/vi/7qvrRWMXY-8/hqdefault.jpg",
                                            "mediaType": "YOUTUBE"
                                          },
                                          {
                                            "url": "https://soundcloud.com/example/drum-performance",
                                            "title": "SoundCloud",
                                            "thumbnailUrl": null,
                                            "mediaType": "SOUNDCLOUD"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-22 18:42:15"
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
                            @ExampleObject(name = "SESSION404_1", ref = "#/components/examples/SESSION404_1")
                    }))
    })
    SuccessResponse<MySessionApplicationDetailResponse> getMySessionApplicationDetail(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "세션 지원서 ID", example = "1") Long sessionApplicationId
    );

    @Operation(tags = "세션", summary = "지원 취소",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "지원이 취소되었습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_17",
                                      "message": "지원이 취소되었습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-22 00:37:44"
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
                            @ExampleObject(name = "SESSION404_3", ref = "#/components/examples/SESSION404_3")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION409_4", ref = "#/components/examples/SESSION409_4")
                    }))
    })
    SuccessResponse<Void> cancelApplicationSubmission(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "제출한 지원 내역 ID", example = "1") Long applicationSubmissionId
    );

    @Operation(tags = "세션", summary = "내 지원 내역 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "내 지원 내역 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.MyApplicationSubmissionListResponseEnvelope.class), examples = {
                            @ExampleObject(name = "데이터 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_16",
                                      "message": "내 지원 내역 조회에 성공했습니다.",
                                      "result": {
                                        "content": [
                                          {
                                            "applicationSubmissionId": 4,
                                            "sessionRecruitmentId": 1,
                                            "sessionApplicationId": 14,
                                            "checkedAt": "2026-07-17T05:40:15",
                                            "status": "지원 완료",
                                            "recruitmentTitle": "드럼 세션 모집",
                                            "bandName": "APPLES",
                                            "appliedAgo": 5
                                          },
                                          {
                                            "applicationSubmissionId": 3,
                                            "sessionRecruitmentId": 5,
                                            "sessionApplicationId": 14,
                                            "checkedAt": "2026-07-11T20:06:54",
                                            "status": "지원 완료",
                                            "recruitmentTitle": "드럼 세션 모집",
                                            "bandName": "APPLES",
                                            "appliedAgo": 11
                                          },
                                          {
                                            "applicationSubmissionId": 2,
                                            "sessionRecruitmentId": 5,
                                            "sessionApplicationId": 13,
                                            "checkedAt": null,
                                            "status": "지원 취소",
                                            "recruitmentTitle": "드럼 세션 모집",
                                            "bandName": "APPLES",
                                            "appliedAgo": 11
                                          }
                                        ],
                                        "size": 10,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-22 01:22:03"
                                    }
                                    """),
                            @ExampleObject(name = "데이터 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_16",
                                      "message": "내 지원 내역 조회에 성공했습니다.",
                                      "result": {
                                        "content": [],
                                        "size": 10,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-22 01:22:03"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<MyApplicationSubmissionListResponse> getMyApplicationSubmissions(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지에서는 생략") Long cursorId,
            @Parameter(description = "페이지 크기") Integer size
    );
}
