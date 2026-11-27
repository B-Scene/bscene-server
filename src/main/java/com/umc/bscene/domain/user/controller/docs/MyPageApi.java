package com.umc.bscene.domain.user.controller.docs;

import com.umc.bscene.domain.user.dto.request.MyInfoUpdateRequest;
import com.umc.bscene.domain.user.dto.request.SessionApplyConfirmRequest;
import com.umc.bscene.domain.user.dto.request.SessionRecruitDecisionRequest;
import com.umc.bscene.domain.user.dto.request.UserModeUpdateRequest;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;
import com.umc.bscene.domain.user.dto.response.InterestedPerformanceResponse;
import com.umc.bscene.domain.user.dto.response.MyInfoResponse;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse;
import com.umc.bscene.domain.user.dto.response.mypage.MyPageResponse;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.enums.HistoryYearFilter;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import org.springframework.http.ResponseEntity;
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

public interface MyPageApi {

    @Operation(tags = "마이", summary = "밴드·팬 프로필 목록 조회",
            description = "type=all이면 팬 프로필도 반환합니다. 기본값 band는 밴드 프로필만 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.MyProfileResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "밴드 및 팬 프로필", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "COMMON_200",
                                              "message": "호출에 성공했습니다.",
                                              "result": {"bandProfiles":[{"bandId":1,"bandMemberProfileId":1,"profileImageUrl":"","bandName":"테스트","genre":"인디","region":"서울","isActive":true},{"bandId":2,"bandMemberProfileId":2,"profileImageUrl":null,"bandName":"실리카겔","genre":"하드록","region":"부산","isActive":false},{"bandId":3,"bandMemberProfileId":3,"profileImageUrl":null,"bandName":"유다빈밴드","genre":"인디","region":"서울","isActive":false}],"fanProfile":{"fanProfileId":1,"profileImageUrl":null,"nickname":"홍길동","email":"test@bscene.com","isActive":true}},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "밴드 프로필", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "COMMON_200",
                                              "message": "호출에 성공했습니다.",
                                              "result": {"bandProfiles":[{"bandId":1,"bandMemberProfileId":1,"profileImageUrl":"","bandName":"테스트","genre":"인디","region":"서울","isActive":true},{"bandId":2,"bandMemberProfileId":2,"profileImageUrl":null,"bandName":"실리카겔","genre":"하드록","region":"부산","isActive":false},{"bandId":3,"bandMemberProfileId":3,"profileImageUrl":null,"bandName":"유다빈밴드","genre":"인디","region":"서울","isActive":false}],"fanProfile":null},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER400_2", ref = "#/components/examples/USER400_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER403_1", ref = "#/components/examples/USER403_1")
                            }))
    })
    ResponseEntity<SuccessResponse<?>> getMyProfiles(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "all", schema = @Schema(allowableValues = {"band", "all"})) String type
    );

    @Operation(tags = "마이", summary = "팬·밴드 모드 전환",
            description = "현재 모드를 전환합니다. HTTP 200의 응답 본문에 status 204와 result null을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_204", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 204,
                                              "code": "COMMON_204",
                                              "message": "호출에 성공했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER404_2", ref = "#/components/examples/USER404_2")
                            }))
    })
    ResponseEntity<SuccessResponse<?>> toggleUserMode(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "마이", summary = "프로필 선택으로 모드 변경",
            description = "선택한 프로필과 모드로 전환합니다. HTTP 200의 응답 본문에 status 204와 result null을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserModeUpdateRequest.class),
                            examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "profileId": 1,
                                              "type": "BAND"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_204", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 204,
                                              "code": "COMMON_204",
                                              "message": "호출에 성공했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "MEMBER403_2", ref = "#/components/examples/PROFILE_ACTIVATION_FORBIDDEN")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER404_2", ref = "#/components/examples/USER404_2")
                            }))
    })
    ResponseEntity<SuccessResponse<?>> patchUserMode(
            @Parameter(hidden = true) AuthMember authMember,
            UserModeUpdateRequest request
    );

    @Operation(tags = "마이", summary = "내 마이페이지 조회",
            description = "현재 모드에 맞는 팬 또는 밴드 마이페이지를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.MyPageResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "팬 모드", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_1",
                                              "message": "팬 모드 마이페이지에 성공했습니다.",
                                              "result": {"nickname":"닉네임","profileImageUrl":"https://cdn.bscene.com/fan/12345/profile.png","genre":"INDIE","additionalGenreCount":1,"regions":["SEOUL"],"currentMode":"FAN","followingCount":128,"interestedPerformanceCount":24,"participatedPerformanceCount":8},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "밴드 모드", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "COMMON_200",
                                              "message": "호출에 성공했습니다.",
                                              "result": {"bandMemberProfileId":1,"nickname":"세진","bandName":"비씬","parts":["드럼"],"currentMode":"BAND","follower":128,"applicant":24,"performance":8,"isBandMember":true},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER404_1", ref = "#/components/examples/USER404_1")
                            }))
    })
    ResponseEntity<SuccessResponse<MyPageResponse>> getMyPage(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "마이", summary = "공연 참여 기록 조회",
            description = "참여 완료 공연을 연도 필터와 0부터 시작하는 페이지로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.ParticipationHistoryResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "USER200_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_2",
                                              "message": "공연 참여 기록을 조회했습니다.",
                                              "result": {"totalCount":12,"appliedFilter":"ALL","baseYear":2026,"items":[{"performanceId":1,"title":"LUCY 단독 콘서트","venue":"올림픽공원 올림픽홀","performanceDate":"2026-05-03","startTime":"18:00:00","posterImageUrl":"https://cdn.bscene.com/performance/lucy.png","status":"COMPLETED"}],"page":0,"hasNext":true},
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
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<ParticipationHistoryResponse>> getPerformanceHistory(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "THIS_YEAR") HistoryYearFilter filter,
            @Parameter(example = "0") int page,
            @Parameter(example = "10") int size
    );

    @Operation(tags = "마이", summary = "관심 공연 목록 조회",
            description = "관심 공연과 참여 상태를 조회합니다. totalCount는 첫 페이지에 제공됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.InterestedPerformanceResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "예시 1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_3",
                                              "message": "관심 공연 목록을 조회했습니다.",
                                              "result": {"totalCount":12,"appliedFilter":"ALL","baseYear":2026,"items":[{"performanceId":1,"title":"LUCY 단독 콘서트","venue":"올림픽공원 올림픽홀","performanceDate":"2026-05-03","startTime":"18:00:00","posterImageUrl":"https://s3.../poster.jpg","participationStatus":"SCHEDULED"},{"performanceId":2,"title":"LUCY 단독 콘서트","venue":"올림픽공원 올림픽홀","performanceDate":"2026-05-03","startTime":"20:00:00","posterImageUrl":"https://s3.../poster2.jpg","participationStatus":null}],"page":0,"hasNext":true},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "예시 2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_3",
                                              "message": "관심 공연 목록을 조회했습니다.",
                                              "result": {"totalCount":null,"appliedFilter":"ALL","baseYear":2026,"items":[{"performanceId":5,"title":"여름밤 어쿠스틱 공연","venue":"롤링홀","performanceDate":"2026-08-19","startTime":"18:30:00","posterImageUrl":"https://s3.../poster5.jpg","participationStatus":"COMPLETED"}],"page":1,"hasNext":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "예시 3", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_3",
                                              "message": "관심 공연 목록을 조회했습니다.",
                                              "result": {"totalCount":0,"appliedFilter":"LAST_YEAR","baseYear":2026,"items":[],"page":0,"hasNext":false},
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
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<InterestedPerformanceResponse>> getInterestedPerformances(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "THIS_YEAR") HistoryYearFilter filter,
            @Parameter(example = "0") int page,
            @Parameter(example = "10") int size
    );

    @Operation(tags = "마이", summary = "팔로우한 밴드 목록 조회",
            description = "팔로우한 밴드를 이름순으로 조회합니다. totalCount는 첫 페이지에 제공됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.FollowedBandResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "예시 1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_6",
                                              "message": "팔로우한 밴드 목록을 조회했습니다.",
                                              "result": {"totalCount":4,"items":[{"bandId":12,"name":"DAYBREAK","genre":"ALTERNATIVE_ROCK","region":"SEOUL","profileImageUrl":"https://cdn.bscene.com/bands/12/profile.jpg","followerCount":8421},{"bandId":7,"name":"LUCY","genre":"INDIE","region":"SEOUL","profileImageUrl":"https://cdn.bscene.com/bands/7/profile.jpg","followerCount":12000},{"bandId":3,"name":"WAVY","genre":"ALTERNATIVE_ROCK","region":"SEOUL","profileImageUrl":"https://cdn.bscene.com/bands/3/profile.jpg","followerCount":560},{"bandId":21,"name":"새소년","genre":"INDIE","region":"SEOUL","profileImageUrl":"https://cdn.bscene.com/bands/21/profile.jpg","followerCount":24000}],"page":0,"hasNext":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "예시 2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_6",
                                              "message": "팔로우한 밴드 목록을 조회했습니다.",
                                              "result": {"totalCount":null,"items":[{"bandId":34,"name":"터치드","genre":"ALTERNATIVE_ROCK","region":"GYEONGGI","profileImageUrl":"https://cdn.bscene.com/bands/34/profile.jpg","followerCount":3100}],"page":1,"hasNext":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "예시 3", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_6",
                                              "message": "팔로우한 밴드 목록을 조회했습니다.",
                                              "result": {"totalCount":0,"items":[],"page":0,"hasNext":false},
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
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<FollowedBandResponse>> getFollowedBands(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "0") int page,
            @Parameter(example = "10") int size
    );

    @Operation(tags = "마이", summary = "내 정보 조회",
            description = "팬 프로필의 닉네임·사진 URL·장르·지역을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.MyInfoResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "USER200_7", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_7",
                                              "message": "내 정보를 조회했습니다.",
                                              "result": {"nickname":"세진","profileImageUrl":"https://cdn.bscene.com/fan/12345/profile.png","genres":["INDIE","JAZZ","HARD_ROCK"],"regions":["SEOUL","GYEONGGI"]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER404_1", ref = "#/components/examples/USER404_1")
                            }))
    })
    ResponseEntity<SuccessResponse<MyInfoResponse>> getMyInfo(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "마이", summary = "내 정보 수정",
            description = "닉네임·관심 장르·활동 지역을 전달합니다. 사진 URL을 생략하거나 null이면 유지하고 빈 문자열이면 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyInfoUpdateRequest.class),
                            examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "nickname": "세진",
                                              "profileImageUrl": "https://cdn.bscene.com/fan/12345/profile.png",
                                              "genres": ["INDIE","JAZZ","HARD_ROCK"],
                                              "regions": ["SEOUL","GYEONGGI"]
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.MyInfoResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "USER200_8", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "USER200_8",
                                              "message": "내 정보를 수정했습니다.",
                                              "result": {"nickname":"세진","profileImageUrl":"https://cdn.bscene.com/fan/12345/profile.png","genres":["INDIE","JAZZ","HARD_ROCK"],"regions":["SEOUL","GYEONGGI"]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                                    @ExampleObject(name = "USER400_3", ref = "#/components/examples/USER400_3")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER404_1", ref = "#/components/examples/USER404_1"),
                                    @ExampleObject(name = "USER404_2", ref = "#/components/examples/USER404_2")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER409_2", ref = "#/components/examples/USER409_2")
                            }))
    })
    ResponseEntity<SuccessResponse<MyInfoResponse>> updateMyInfo(
            @Parameter(hidden = true) AuthMember authMember,
            MyInfoUpdateRequest request
    );

    @Operation(tags = "마이", summary = "받은 세션 지원 조회",
            description = "활성 밴드의 공고별 지원자를 반환합니다. status는 OPEN(진행 중) 또는 CLOSE(마감)입니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.ReceivedRecruitmentsEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_200", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "COMMON_200",
                                              "message": "호출에 성공했습니다.",
                                              "result": {"items":[{"recruitmentPostId":1,"dueDate":"2027-08-08 18:00:00","title":"드럼 세션 모집","part":"드럼","genre":"인디","region":"서울","totalApplicants":1,"recruiters":[{"applySubmissionId":1,"profileImageUrl":null,"name":"세진","part":"드럼","level":"중급","region":"서울","status":"PENDING","isChecked":false}]}],"pageInfo":{"nextCursor":null,"hasNext":false}},
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
                                    @ExampleObject(name = "BAND403_3", ref = "#/components/examples/BAND403_3")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "BAND404_2", ref = "#/components/examples/BAND404_2")
                            }))
    })
    ResponseEntity<SuccessResponse<CursorPage<SessionRecruitmentResponse>>> getReceiveRecruitments(
            @Parameter(hidden = true) AuthMember user,
            @Parameter(example = "1") RecruitmentStatusFilter status,
            @Parameter(example = "25") Long cursor,
            @Parameter(example = "10") int size
    );

    @Operation(tags = "마이", summary = "밴드의 세션 지원 수락·거절",
            description = "밴드가 지원을 수락 또는 거절합니다. 수락 시 지원자의 최종 결정이 필요합니다. HTTP 200의 응답 본문에 status 204가 담깁니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SessionRecruitDecisionRequest.class),
                            examples = {
                                    @ExampleObject(name = "수락", value = """
                                            {
                                              "isApproved": true
                                            }
                                            """),
                                    @ExampleObject(name = "거절", value = """
                                            {
                                              "isApproved": false
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_204", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 204,
                                              "code": "COMMON_204",
                                              "message": "호출에 성공했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION403_3", ref = "#/components/examples/SESSION403_3"),
                                    @ExampleObject(name = "BAND403_3", ref = "#/components/examples/BAND403_3"),
                                    @ExampleObject(name = "BAND403_4", ref = "#/components/examples/BAND403_4")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER404_2", ref = "#/components/examples/USER404_2"),
                                    @ExampleObject(name = "SESSION404_3", ref = "#/components/examples/SESSION404_3")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION409_5", ref = "#/components/examples/SESSION409_5")
                            }))
    })
    ResponseEntity<SuccessResponse<?>> decideSessionApply(
            @Parameter(hidden = true) AuthMember member,
            @Parameter(example = "1") Long applySubmissionId,
            SessionRecruitDecisionRequest request
    );

    @Operation(tags = "마이", summary = "지원자의 세션 활동 최종 결정",
            description = "최종 수락 시 활동명과 파트가 필수입니다. 거절하면 bandMemberProfileId가 null입니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SessionApplyConfirmRequest.class),
                            examples = {
                                    @ExampleObject(name = "최종 수락", value = """
                                            {
                                              "isAccepted": true,
                                              "nickname": "홍길동",
                                              "part": "DRUM"
                                            }
                                            """),
                                    @ExampleObject(name = "최종 거절", value = """
                                            {
                                              "isAccepted": false
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "최종 결정 처리 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.SessionApplyConfirmResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "최종 수락", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 201,
                                              "code": "COMMON_201",
                                              "message": "리소스 생성에 성공했습니다.",
                                              "result": {"bandMemberProfileId":1},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "최종 거절", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 201,
                                              "code": "COMMON_201",
                                              "message": "리소스 생성에 성공했습니다.",
                                              "result": {"bandMemberProfileId":null},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "USER400_2", ref = "#/components/examples/USER400_2"),
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION404_2", ref = "#/components/examples/SESSION404_2"),
                                    @ExampleObject(name = "SESSION404_3", ref = "#/components/examples/SESSION404_3"),
                                    @ExampleObject(name = "USER404_2", ref = "#/components/examples/USER404_2")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SESSION409_6", ref = "#/components/examples/SESSION409_6")
                            }))
    })
    ResponseEntity<SuccessResponse<?>> confirmSessionApply(
            @Parameter(hidden = true) AuthMember member,
            @Parameter(example = "1") Long applySubmissionId,
            SessionApplyConfirmRequest request
    );
}
