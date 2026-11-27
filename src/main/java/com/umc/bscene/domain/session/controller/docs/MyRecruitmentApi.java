package com.umc.bscene.domain.session.controller.docs;

import com.umc.bscene.domain.session.dto.application.response.SubmittedApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentUpdateRequest;
import com.umc.bscene.domain.session.dto.recruitment.response.ManagedRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentEditResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

public interface MyRecruitmentApi {

    @Operation(tags = "마이", summary = "공고에 제출된 지원서 조회",
            description = "모집 밴드가 제출받은 지원서와 공고 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.SubmittedApplicationDetailResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "SESSION200_18", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SESSION200_18",
                                              "message": "제출된 지원서 조회에 성공했습니다.",
                                              "result": {"applicationSubmissionId":8,"sessionRecruitmentId":8,"recruitmentTitle":"드럼 세션 모집","bandId":5,"bandName":"apple Band","isOwner":true,"deadlineAt":"2027-08-07T18:00:00","sessionApplicationId":53,"title":"인디밴드 드럼 지원서","userId":1,"profileImageUrl":"https://example.com/session-profile.jpg","nickname":"홍길동","defaultPart":"드럼","defaultSkillLevel":"중급","defaultRegion":"서울","isPublic":true,"purpose":"기본","oneLineIntro":"선명한 리듬과 안정적인 합주","intro":"안녕하세요. 드럼 세션으로 활동 중인 정하람입니다. 정기 합주와 소규모 라이브 경험을 바탕으로 밴드의 전체 사운드를 해치지 않으면서 안정적으로 리듬을 잡는 편입니다. 인디팝과 록 장르를 주로 연주하며 공연과 음원 작업 일정에 꾸준히 참여할 수 있습니다.","part":"드럼","skillLevel":"중급","genre":"인디","region":"서울","availableActivities":["정기 합주","라이브 공연","앨범 및 음원 작업"],"careers":[{"sessionApplicationCareerId":95,"name":"B밴드 드러머 활동","period":"2024.06 ~ 현재","description":"정기 합주, 소규모 라이브 8회, 공연 세트리스트 구성과 리듬 편곡에 참여했습니다."},{"sessionApplicationCareerId":96,"name":"홍대 라이브클럽 드럼 세션","period":"2025.09","description":"A밴드 단독 공연의 드럼 세션으로 참여했습니다."}],"portfolioLinks":[{"sessionApplicationLinkId":131,"url":"https://youtu.be/7qvrRWMXY-8?si=QnSZnEevCxlcSDyP","title":"LUCY(루시) - 개화 (Flowering) [가사/Lyrics]","thumbnailUrl":"https://img.youtube.com/vi/7qvrRWMXY-8/hqdefault.jpg","mediaType":"YOUTUBE"},{"sessionApplicationLinkId":132,"url":"https://soundcloud.com/example/drum-performance","title":"SoundCloud","thumbnailUrl":null,"mediaType":"SOUNDCLOUD"}]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION404_3", ref = "#/components/examples/SESSION404_3")
                            }))
    })
    SuccessResponse<SubmittedApplicationDetailResponse> getSubmittedApplication(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "12") Long applicationSubmissionId
    );

    @Operation(tags = "마이", summary = "내 밴드 모집 공고 관리 조회",
            description = "밴드 오너가 자신의 공고를 조회합니다. 다음 페이지는 nextCursor를 cursorId로 전달합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.ManagedRecruitmentListResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "SESSION200_23", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SESSION200_23",
                                              "message": "모집 공고 관리 목록 조회에 성공했습니다.",
                                              "result": {"bandId":1,"bandName":"WAVY","bandProfileImageUrl":"https://example.com/bands/wavy-profile.jpg","content":[{"sessionRecruitmentId":27,"dDay":18,"recruitmentTitle":"드럼 세션 구합니다","bandName":"WAVY","bandGenre":"인디","bandRegion":"서울","postedAgo":2,"summary":"장기적으로 함께 활동할 드러머를 찾습니다."},{"sessionRecruitmentId":26,"dDay":18,"recruitmentTitle":"드럼 세션 구합니다","bandName":"WAVY","bandGenre":"인디","bandRegion":"서울","postedAgo":2,"summary":"라이브와 앨범 작업 경험자를 우대합니다."}],"size":10,"nextCursor":null,"hasNext":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION403_1", ref = "#/components/examples/SESSION403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            }))
    })
    SuccessResponse<ManagedRecruitmentListResponse> getManagedRecruitments(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            @Parameter(example = "25") Long cursorId,
            @Parameter(example = "10") Integer size
    );

    @Operation(tags = "마이", summary = "모집 공고 수정용 정보 조회",
            description = "밴드 오너가 수정 화면에 표시할 현재 값을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.SessionRecruitmentEditResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "SESSION200_24", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SESSION200_24",
                                              "message": "모집 공고 수정 정보 조회에 성공했습니다.",
                                              "result": {"recruitmentTitle":"드럼 세션 모집","summary":"함께 공연할 드러머를 모집합니다.","content":"정기적으로 함께 공연하고 합주할 드러머를 모집합니다. 공연 및 합주 경험이 있는 분을 우대합니다.","part":"드럼","skillLevel":"입문","genre":"인디","region":"서울","practiceSchedule":"매주 토요일 18시","practicePlace":"홍대 합주실","deadlineAt":"2027-08-07T18:00:00","qualification":"합주 경험 1년 이상"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION403_1", ref = "#/components/examples/SESSION403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION404_2", ref = "#/components/examples/SESSION404_2")
                            }))
    })
    SuccessResponse<SessionRecruitmentEditResponse> getRecruitmentForEdit(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "8") Long sessionRecruitmentId
    );

    @Operation(tags = "마이", summary = "내 세션 모집 공고 수정",
            description = "밴드 오너가 모집 공고를 수정합니다. 마감 일시는 현재보다 이후여야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SessionRecruitmentUpdateRequest.class),
                            examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
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
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.SessionRecruitmentCreateResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "SESSION200_11", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SESSION200_11",
                                              "message": "세션 모집 공고 수정에 성공했습니다.",
                                              "result": {"sessionRecruitmentId":3,"bandId":1,"recruitmentTitle":"드럼 세션 모집","summary":"함께 공연할 드러머를 모집합니다.","content":"정기적으로 함께 공연하고 합주할 드러머를 모집합니다. 공연 및 합주 경험이 있는 분을 우대합니다.","part":"드럼","skillLevel":"입문","genre":"인디","region":"서울","practiceSchedule":"매주 토요일 18시","practicePlace":"홍대 합주실","deadlineAt":"2027-08-07T18:00:00","qualification":"합주 경험 1년 이상","createdAt":"2026-07-10T12:17:14.843595"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION400_4", ref = "#/components/examples/SESSION400_4"),
                                    @ExampleObject(name = "SESSION400_5", ref = "#/components/examples/SESSION400_5")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION403_1", ref = "#/components/examples/SESSION403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION404_2", ref = "#/components/examples/SESSION404_2")
                            }))
    })
    SuccessResponse<SessionRecruitmentCreateResponse> updateSessionRecruitment(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "8") Long sessionRecruitmentId,
            @Valid SessionRecruitmentUpdateRequest request
    );

    @Operation(tags = "마이", summary = "내 세션 모집 공고 삭제",
            description = "밴드 오너가 모집 공고를 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "SESSION200_12", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SESSION200_12",
                                              "message": "세션 모집 공고 삭제에 성공했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION403_1", ref = "#/components/examples/SESSION403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION404_2", ref = "#/components/examples/SESSION404_2")
                            }))
    })
    SuccessResponse<Void> deleteSessionRecruitment(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "8") Long sessionRecruitmentId
    );
}
