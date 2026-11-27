package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.FollowerBlock;
import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandUpdateRequest;
import com.umc.bscene.domain.band.dto.response.BandNameCheckResponse;
import com.umc.bscene.domain.band.dto.response.BandProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import org.springframework.http.ResponseEntity;
import com.umc.bscene.global.swagger.BandApiResponseSchemas;
import com.umc.bscene.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public interface BandProfileApi {

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 생성",
            description = "본인 멤버 프로필로 밴드를 생성하고 검수를 요청합니다. 같은 이름의 검수 중 밴드가 있으면 충돌이 발생합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandCreateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "name": "SAMSUNG",
                                              "genre": "METAL",
                                              "region": "SEOUL",
                                              "bandMemberProfileId": 1,
                                              "profileImageUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/band/apple.png",
                                              "description": "저희는 락 · 서울 · 4인조 그룹입니다."
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.BandCreationEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND201_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 201,
                                              "code": "BAND201_1",
                                              "message": "밴드가 생성되었습니다.",
                                              "result": {"id":1,"ownerId":1,"name":"SAMSUNG","genre":"METAL","region":"SEOUL","profileImageUrl":"https://bucket.s3.ap-northeast-2.amazonaws.com/band/apple.png","description":"저희는 락 · 서울 · 4인조 그룹입니다."},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                                    @ExampleObject(name = "PROFILE400_1", ref = "#/components/examples/PROFILE400_1")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND409_2", ref = "#/components/examples/BAND409_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandResponse>> createBand(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid BandCreateRequest request
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드명 중복 확인",
            description = "같은 이름의 검수 중 밴드 존재 여부를 확인합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.NameCheckEnvelope.class), examples = {
                                    @ExampleObject(name = "사용 가능", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_6",
                                              "message": "사용 가능한 밴드명이에요.",
                                              "result": {"available":true},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "중복", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_7",
                                              "message": "이미 사용 중인 밴드명이에요.",
                                              "result": {"available":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandNameCheckResponse>> checkBandName(
            @Parameter(example = "SAMSUNG") String name
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 프로필 조회",
            description = "밴드 정보와 팔로워·멤버·공연 수를 조회합니다. 검수 중인 밴드는 소속 멤버만 볼 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.BandProfileEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_8", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_8",
                                              "message": "밴드 프로필을 조회했습니다.",
                                              "result": {"bandId":1,"name":"APPLE","genre":"ALTERNATIVE_ROCK","region":"SEOUL","profileImageUrl":"https://bucket.s3.ap-northeast-2.amazonaws.com/band/apple.png","description":"락 · 서울 · 4인조","followerCount":0,"memberCount":1,"performanceCount":0},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandProfileResponse>> getBandProfile(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 프로필 수정",
            description = "해당 밴드의 오너만 밴드 프로필을 수정할 수 있습니다. 사진 삭제 시 deleteProfileImage를 true로 전달하고 새 사진 URL은 생략합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandUpdateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "name": "APPLES",
                                              "genre": "ALTERNATIVE_ROCK",
                                              "region": "SEOUL",
                                              "profileImageUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/band/apple.png",
                                              "description": "락 · 서울 · 4인조"
                                            }
                                            """),
                                    @ExampleObject(name = "사진 삭제", value = """
                                            {
                                              "deleteProfileImage": true
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.BandProfileEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_9", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_9",
                                              "message": "밴드 프로필을 수정했습니다.",
                                              "result": {"bandId":1,"name":"APPLEs","genre":"ALTERNATIVE_ROCK","region":"SEOUL","profileImageUrl":"https://bucket.s3.ap-northeast-2.amazonaws.com/band/apple.png","description":"락 · 서울 · 4인조","followerCount":0,"memberCount":1,"performanceCount":0},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                                    @ExampleObject(name = "BAND400_4", ref = "#/components/examples/BAND400_4")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND403_1", ref = "#/components/examples/BAND403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND409_2", ref = "#/components/examples/BAND409_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandProfileResponse>> updateBandProfile(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            BandUpdateRequest request
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "내 밴드 팔로워 조회",
            description = "현재 활성 밴드의 팔로워를 조회합니다. 다음 페이지는 nextCursor를 cursor로 보냅니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.FollowersEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_16", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_16",
                                              "message": "밴드 팔로워 목록을 조회했습니다.",
                                              "result": {"items":[{"userId":4,"fanProfileImageUrl":null,"nickname":"양서윤양서윤"},{"userId":16,"fanProfileImageUrl":null,"nickname":"뚤범"},{"userId":3,"fanProfileImageUrl":null,"nickname":"하이이잉"},{"userId":17,"fanProfileImageUrl":null,"nickname":"ㅎㅎ"}],"pageInfo":{"nextCursor":null,"hasNext":false}},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND403_3", ref = "#/components/examples/BAND403_3"),
                                    @ExampleObject(name = "BAND403_4", ref = "#/components/examples/BAND403_4")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_2", ref = "#/components/examples/BAND404_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<CursorPage<FollowerBlock>>> getMyBandFollowers(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "25") Long cursor,
            @Parameter(example = "10") @Min(1) @Max(15) Integer size
    );
}
