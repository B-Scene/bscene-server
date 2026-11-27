package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.request.BandMemberProfileCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberProfileUpdateRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberProfileResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import java.util.List;
import org.springframework.http.ResponseEntity;
import com.umc.bscene.global.swagger.BandApiResponseSchemas;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
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

public interface BandMemberProfileApi {

    @Operation(tags = "밴드 멤버 프로필", summary = "밴드 멤버 프로필 생성",
            description = "닉네임과 파트로 내 멤버 프로필을 생성합니다. 활성 프로필이 없으면 새 프로필이 활성화됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandMemberProfileCreateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "nickname": "지훈기타",
                                              "part": "GUITAR"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.ProfileEnvelope.class), examples = {
                                    @ExampleObject(name = "PROFILE201_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 201,
                                              "code": "PROFILE201_1",
                                              "message": "멤버 프로필을 생성했습니다.",
                                              "result": {"id":1,"nickname":"지훈기타","part":"기타","active":true,"createdAt":"2026-07-16T14:30:00"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandMemberProfileResponse>> createProfile(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid BandMemberProfileCreateRequest request
    );

    @Operation(tags = "밴드 멤버 프로필", summary = "내 밴드 멤버 프로필 목록 조회",
            description = "로그인한 사용자가 보유한 밴드 멤버 프로필 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.ProfileListEnvelope.class), examples = {
                                    @ExampleObject(name = "PROFILE200_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "PROFILE200_1",
                                              "message": "멤버 프로필 목록을 조회했습니다.",
                                              "result": [{"id":1,"nickname":"지훈기타","part":"기타","active":true,"createdAt":"2026-07-16T14:30:00"},{"id":2,"nickname":"지훈보컬","part":"보컬","active":false,"createdAt":"2026-07-16T15:00:00"}],
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<List<BandMemberProfileResponse>>> getProfiles(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "밴드 멤버 프로필", summary = "내 활성 밴드 멤버 프로필 조회",
            description = "현재 활성화된 멤버 프로필을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.ProfileEnvelope.class), examples = {
                                    @ExampleObject(name = "PROFILE200_6", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "PROFILE200_6",
                                              "message": "활성 프로필을 조회했습니다.",
                                              "result": {"id":1,"nickname":"지훈기타","part":"기타","active":true,"createdAt":"2026-07-16T14:30:00"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_4", ref = "#/components/examples/BAND404_4")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandMemberProfileResponse>> getActiveProfile(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "밴드 멤버 프로필", summary = "밴드 멤버 프로필 단건 조회",
            description = "프로필 ID로 본인 프로필을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.ProfileEnvelope.class), examples = {
                                    @ExampleObject(name = "PROFILE200_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "PROFILE200_2",
                                              "message": "멤버 프로필을 조회했습니다.",
                                              "result": {"id":2,"nickname":"지훈보컬","part":"보컬","active":false,"createdAt":"2026-07-16T15:00:00"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_4", ref = "#/components/examples/BAND404_4")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandMemberProfileResponse>> getProfile(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long profileId
    );

    @Operation(tags = "밴드 멤버 프로필", summary = "밴드 멤버 프로필 수정",
            description = "본인 프로필의 닉네임 또는 파트를 수정합니다. 생략한 값은 유지됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandMemberProfileUpdateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "nickname": "새닉네임",
                                              "part": "DRUM"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.ProfileEnvelope.class), examples = {
                                    @ExampleObject(name = "PROFILE200_3", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "PROFILE200_3",
                                              "message": "멤버 프로필을 수정했습니다.",
                                              "result": {"id":2,"nickname":"새닉네임","part":"드럼","active":false,"createdAt":"2026-07-16T15:00:00"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_4", ref = "#/components/examples/BAND404_4")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandMemberProfileResponse>> updateProfile(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long profileId,
            @Valid BandMemberProfileUpdateRequest request
    );

    @Operation(tags = "밴드 멤버 프로필", summary = "밴드 멤버 프로필 삭제",
            description = "내 멤버 프로필을 삭제합니다. 밴드에서 사용 중인 프로필은 삭제할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                                    @ExampleObject(name = "PROFILE200_4", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "PROFILE200_4",
                                              "message": "멤버 프로필을 삭제했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_4", ref = "#/components/examples/BAND404_4")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "PROFILE409_1", ref = "#/components/examples/PROFILE409_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> deleteProfile(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long profileId
    );

    @Operation(tags = "밴드 멤버 프로필", summary = "활성 밴드 멤버 프로필 전환",
            description = "선택한 본인 프로필을 활성화하고 이전 프로필은 비활성화합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.ProfileEnvelope.class), examples = {
                                    @ExampleObject(name = "PROFILE200_5", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "PROFILE200_5",
                                              "message": "활성 프로필을 전환했습니다.",
                                              "result": {"id":2,"nickname":"새닉네임","part":"드럼","active":true,"createdAt":"2026-07-16T15:00:00"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_4", ref = "#/components/examples/BAND404_4")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandMemberProfileResponse>> activateProfile(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long profileId
    );
}
