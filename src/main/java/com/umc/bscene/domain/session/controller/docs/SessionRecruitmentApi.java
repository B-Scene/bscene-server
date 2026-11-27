package com.umc.bscene.domain.session.controller.docs;

import com.umc.bscene.domain.session.dto.application.request.SessionApplicationSubmitRequest;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSubmitResponse;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentCreateRequest;
import com.umc.bscene.domain.session.dto.recruitment.response.InterestedRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.RecentRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.RecruitmentSearchKeywordResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionRecruitmentSortType;
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

import java.util.List;

public interface SessionRecruitmentApi {

    @Operation(tags = "세션", summary = "모집 공고 등록",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SessionRecruitmentCreateRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "bandMemberId": 1,
                                      "recruitmentTitle": "드럼 세션 모집",
                                      "summary": "함께 공연할 드러머를 모집합니다.",
                                      "content": "정기적으로 함께 공연하고 합주할 드러머를 모집합니다. 공연 및 합주 경험이 있는 분을 우대합니다.",
                                      "part": "드럼",
                                      "skillLevel": "입문",
                                      "genre": "인디",
                                      "region": "서울",
                                      "practiceSchedule": "매주 토요일 18시",
                                      "practicePlace": "홍대 합주실",
                                      "deadlineAt": "2027-08-07T18:00:00",
                                      "qualification": "합주 경험 1년 이상"
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "성공 (HTTP 200, 응답 본문 status 201)", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.SessionRecruitmentCreateResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "SESSION201_2",
                                      "message": "세션 모집 공고 등록에 성공했습니다.",
                                      "result": {
                                        "sessionRecruitmentId": 24,
                                        "bandId": 1,
                                        "recruitmentTitle": "드럼 세션 모집",
                                        "summary": "함께 공연할 드러머를 모집합니다.",
                                        "content": "정기적으로 함께 공연하고 합주할 드러머를 모집합니다. 공연 및 합주 경험이 있는 분을 우대합니다.",
                                        "part": "드럼",
                                        "skillLevel": "입문",
                                        "genre": "인디",
                                        "region": "서울",
                                        "practiceSchedule": "매주 토요일 18시",
                                        "practicePlace": "홍대 합주실",
                                        "deadlineAt": "2027-08-07T18:00:00",
                                        "qualification": "합주 경험 1년 이상",
                                        "createdAt": "2026-07-21T03:38:04.3523658"
                                      },
                                      "timeStamp": "2026-07-21 03:38:04"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION400_4", ref = "#/components/examples/SESSION400_4"),
                            @ExampleObject(name = "SESSION400_5", ref = "#/components/examples/SESSION400_5"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION403_1", ref = "#/components/examples/SESSION403_1"),
                            @ExampleObject(name = "BAND403_6", ref = "#/components/examples/BAND403_6")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "BAND404_2", ref = "#/components/examples/BAND404_2")
                    }))
    })
    SuccessResponse<SessionRecruitmentCreateResponse> createSessionRecruitment(
            @Parameter(hidden = true) AuthMember authMember,
            SessionRecruitmentCreateRequest request
    );

    @Operation(tags = "세션", summary = "모집 공고 상세 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 모집 공고 상세 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionRecruitmentDetailResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_13",
                                      "message": "세션 모집 공고 상세 조회에 성공했습니다.",
                                      "result": {
                                        "sessionRecruitmentId": 3,
                                        "isNew": false,
                                        "recruitmentTitle": "드럼 세션 모집",
                                        "deadlineAt": "2026-08-07T18:00:00",
                                        "dDay": 17,
                                        "content": "정기적으로 함께 공연하고 합주할 드러머를 모집합니다. 공연 및 합주 경험이 있는 분을 우대합니다.",
                                        "part": "etc",
                                        "skillLevel": "입문",
                                        "genre": "인디",
                                        "region": "서울",
                                        "practiceSchedule": "매주 토요일 18시",
                                        "practicePlace": "홍대 합주실",
                                        "qualification": "합주 경험 1년 이상",
                                        "bandId": 1,
                                        "bandName": "APPLES",
                                        "bandProfileImageUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/band/apple.png",
                                        "bandGenre": "인디",
                                        "bandRegion": "서울"
                                      },
                                      "timeStamp": "2026-07-21 14:56:19"
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
                            @ExampleObject(name = "SESSION404_2", ref = "#/components/examples/SESSION404_2")
                    }))
    })
    SuccessResponse<SessionRecruitmentDetailResponse> getSessionRecruitmentDetail(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "모집 공고 ID", example = "1") Long sessionRecruitmentId
    );

    @Operation(tags = "세션", summary = "모집 공고 목록 조회",
            description = "파트, 실력대, 장르, 지역, 제목 검색어로 모집 공고를 찾습니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 모집 공고 목록 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionRecruitmentListResponseEnvelope.class), examples = {
                            @ExampleObject(name = "데이터 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_10",
                                      "message": "세션 모집 공고 목록 조회에 성공했습니다.",
                                      "result": {
                                        "content": [
                                          {
                                            "sessionRecruitmentId": 6,
                                            "bandId": 1,
                                            "dDay": 22,
                                            "isNew": true,
                                            "isInterested": true,
                                            "recruitmentTitle": "드럼 세션 모집",
                                            "bandName": "APPLES",
                                            "bandGenre": "인디",
                                            "bandRegion": "서울",
                                            "postedAgo": 0,
                                            "summary": "함께 공연할 드러머를 모집합니다.",
                                            "part": "드럼",
                                            "skillLevel": "입문"
                                          },
                                          {
                                            "sessionRecruitmentId": 7,
                                            "bandId": 1,
                                            "dDay": 23,
                                            "isNew": true,
                                            "isInterested": true,
                                            "recruitmentTitle": "드럼 세션 모집",
                                            "bandName": "WAVY",
                                            "bandGenre": "인디",
                                            "bandRegion": "서울",
                                            "postedAgo": 1,
                                            "summary": "함께 공연할 드러머를 모집합니다.",
                                            "part": "드럼",
                                            "skillLevel": "입문"
                                          },
                                          {
                                            "sessionRecruitmentId": 8,
                                            "bandId": 1,
                                            "dDay": 25,
                                            "isNew": true,
                                            "isInterested": true,
                                            "recruitmentTitle": "드럼 세션 모집",
                                            "bandName": "APPLES",
                                            "bandGenre": "인디",
                                            "bandRegion": "서울",
                                            "postedAgo": 3,
                                            "summary": "함께 공연할 드러머를 모집합니다.",
                                            "part": "드럼",
                                            "skillLevel": "입문"
                                          }
                                        ],
                                        "size": 10,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-08 01:17:43"
                                    }
                                    """),
                            @ExampleObject(name = "데이터 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_10",
                                      "message": "세션 모집 공고 목록 조회에 성공했습니다.",
                                      "result": {
                                        "content": [],
                                        "hasNext": false,
                                        "nextCursor": null,
                                        "size": 10
                                      },
                                      "timeStamp": "2026-07-07 23:04:36"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<SessionRecruitmentListResponse> getSessionRecruitments(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "파트", example = "드럼") Part part,
            @Parameter(description = "실력대", example = "중급") SkillLevel skillLevel,
            @Parameter(description = "장르", example = "인디") String genre,
            @Parameter(description = "활동 지역", example = "서울") String region,
            @Parameter(description = "검색어", example = "드럼") String keyword,
            @Parameter(description = "정렬 방식", example = "LATEST") SessionRecruitmentSortType sort,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지에서는 생략") Long cursorId,
            @Parameter(description = "페이지 크기") Integer size
    );

    @Operation(tags = "세션", summary = "최근 검색어 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 최근검색어 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.RecruitmentSearchKeywordListEnvelope.class), examples = {
                            @ExampleObject(name = "데이터 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_21",
                                      "message": "세션 최근검색어 조회에 성공했습니다.",
                                      "result": [
                                        {
                                          "keywordId": 2,
                                          "keyword": "민정"
                                        },
                                        {
                                          "keywordId": 1,
                                          "keyword": "드럼"
                                        }
                                      ],
                                      "timeStamp": "2026-07-17 04:21:43"
                                    }
                                    """),
                            @ExampleObject(name = "데이터 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_21",
                                      "message": "세션 최근검색어 조회에 성공했습니다.",
                                      "result": [],
                                      "timeStamp": "2026-07-17 04:21:43"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<List<RecruitmentSearchKeywordResponse>> getSearchHistory(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "세션", summary = "최근 검색어 삭제",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 최근검색어 삭제에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_22",
                                      "message": "세션 최근검색어 삭제에 성공했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-21 16:52:16"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<Void> deleteSearchHistory(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "최근 검색어 ID", example = "1") Long keywordId
    );

    @Operation(tags = "세션", summary = "모집 공고에 지원",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SessionApplicationSubmitRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "sessionApplicationId": 3
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "성공 (HTTP 200, 응답 본문 status 201)", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionApplicationSubmitResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "SESSION201_3",
                                      "message": "지원이 완료되었습니다.",
                                      "result": {
                                        "applicationSubmissionId": 1,
                                        "recruitmentTitle": "드럼 세션 모집",
                                        "bandName": "apple Band",
                                        "applicationTitle": "인디밴드 드럼 지원서"
                                      },
                                      "timeStamp": "2026-07-22 00:26:22"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION400_4", ref = "#/components/examples/SESSION400_4"),
                            @ExampleObject(name = "SESSION400_8", ref = "#/components/examples/SESSION400_8"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION403_2", ref = "#/components/examples/SESSION403_2")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION404_2", ref = "#/components/examples/SESSION404_2"),
                            @ExampleObject(name = "SESSION404_1", ref = "#/components/examples/SESSION404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION409_3", ref = "#/components/examples/SESSION409_3")
                    }))
    })
    SuccessResponse<SessionApplicationSubmitResponse> submitApplication(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "모집 공고 ID", example = "1") Long sessionRecruitmentId,
            SessionApplicationSubmitRequest request
    );

    @Operation(tags = "세션", summary = "관심 공고 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "관심 공고 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.InterestedRecruitmentListResponseEnvelope.class), examples = {
                            @ExampleObject(name = "데이터 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_19",
                                      "message": "관심 공고 조회에 성공했습니다.",
                                      "result": {
                                        "content": [
                                          {
                                            "interestId": 8,
                                            "sessionRecruitmentId": 13,
                                            "dDay": 18,
                                            "isClosed": false,
                                            "isInterested": true,
                                            "recruitmentTitle": "드럼 세션 구합니다",
                                            "bandName": "WAVY",
                                            "bandGenre": "인디",
                                            "bandRegion": "서울",
                                            "postedAgo": 2,
                                            "summary": "장기적으로 함께 활동할 드러머를 찾습니다. 라이브와 앨범 작업 경험자 우대",
                                            "part": "드럼",
                                            "skillLevel": "중급"
                                          },
                                          {
                                            "interestId": 7,
                                            "sessionRecruitmentId": 12,
                                            "dDay": 0,
                                            "isClosed": true,
                                            "isInterested": true,
                                            "recruitmentTitle": "공연 객원 기타 모집",
                                            "bandName": "새벽공방",
                                            "bandGenre": "포크록",
                                            "bandRegion": "서울",
                                            "postedAgo": 5,
                                            "summary": "공연에 함께할 객원 기타 세션을 모집합니다.",
                                            "part": "기타",
                                            "skillLevel": "상급"
                                          }
                                        ],
                                        "size": 10,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-22 02:10:00"
                                    }
                                    """),
                            @ExampleObject(name = "데이터 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_19",
                                      "message": "관심 공고 조회에 성공했습니다.",
                                      "result": {
                                        "content": [],
                                        "size": 10,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-11 21:10:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<InterestedRecruitmentListResponse> getInterestedRecruitments(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지에서는 생략") Long cursorId,
            @Parameter(description = "페이지 크기") Integer size
    );

    @Operation(tags = "세션", summary = "최근 본 공고 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "최근 본 공고 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.RecentRecruitmentListResponseEnvelope.class), examples = {
                            @ExampleObject(name = "데이터 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_20",
                                      "message": "최근 본 공고 조회에 성공했습니다.",
                                      "result": {
                                        "content": [
                                          {
                                            "viewId": 12,
                                            "sessionRecruitmentId": 15,
                                            "dDay": -1,
                                            "isClosed": true,
                                            "isInterested": false,
                                            "recruitmentTitle": "드럼 세션 모집",
                                            "bandName": "APPLEs",
                                            "bandGenre": "etc",
                                            "bandRegion": "서울",
                                            "postedAgo": 11,
                                            "summary": "함께 공연할 드러머를 모집합니다.",
                                            "part": "드럼",
                                            "skillLevel": "입문"
                                          },
                                          {
                                            "viewId": 11,
                                            "sessionRecruitmentId": 14,
                                            "dDay": 7,
                                            "isClosed": false,
                                            "isInterested": true,
                                            "recruitmentTitle": "공연 객원 기타 모집합니다",
                                            "bandName": "APPLEs",
                                            "bandGenre": "etc",
                                            "bandRegion": "서울",
                                            "postedAgo": 12,
                                            "summary": "함께 공연할 드러머를 모집합니다.",
                                            "part": "드럼",
                                            "skillLevel": "입문"
                                          }
                                        ],
                                        "size": 10,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-20 03:40:00"
                                    }
                                    """),
                            @ExampleObject(name = "데이터 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_20",
                                      "message": "최근 본 공고 조회에 성공했습니다.",
                                      "result": {
                                        "content": [],
                                        "size": 10,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-11 22:40:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<RecentRecruitmentListResponse> getRecentRecruitments(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지에서는 생략") Long cursorId,
            @Parameter(description = "페이지 크기") Integer size
    );
}
