package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.request.BandMemberInviteRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberProfileCreateRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberAcceptResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberSearchItem;
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

public interface BandMemberApi {

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 멤버·세션 초대",
            description = "밴드 오너가 사용자를 멤버 또는 세션으로 초대합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandMemberInviteRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "userId": 42,
                                              "memberType": "MEMBER"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.MembershipEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND201_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 201,
                                              "code": "BAND201_2",
                                              "message": "밴드 멤버를 초대했습니다.",
                                              "result": {"id":10,"bandId":1,"userId":42,"bandMemberProfileId":null,"profileNickname":null,"memberType":"MEMBER","status":"INVITED","createdAt":"2026-07-25T22:55:09.004162203","part":null,"owner":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "MEMBER403_2", ref = "#/components/examples/BAND_MEMBER_INVITE_FORBIDDEN")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1"),
                                    @ExampleObject(name = "BAND404_3", ref = "#/components/examples/BAND404_3")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "MEMBER409_1", ref = "#/components/examples/MEMBER409_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandMemberResponse>> inviteMember(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            @Valid BandMemberInviteRequest request
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 멤버 목록 조회",
            description = "멤버 목록과 각 멤버의 파트, 역할 및 상태를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.MemberListEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_3", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_3",
                                              "message": "밴드 멤버 목록을 조회했습니다.",
                                              "result": [{"id":1,"bandId":10,"userId":1,"bandMemberProfileId":100,"profileNickname":"오너닉네임","part":"기타","owner":true,"memberType":"MEMBER","status":"ACCEPTED","createdAt":"2026-08-01T10:00:00"},{"id":2,"bandId":10,"userId":2,"bandMemberProfileId":null,"profileNickname":null,"part":null,"owner":false,"memberType":"SESSION","status":"INVITED","createdAt":"2026-08-02T15:30:00"}],
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
    ResponseEntity<SuccessResponse<List<BandMemberResponse>>> getMembers(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 초대 수락",
            description = "받은 초대를 수락하고 입력한 닉네임과 파트로 밴드 내 멤버 프로필을 등록합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandMemberProfileCreateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "nickname": "드럼천재",
                                              "part": "DRUM"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.MemberAcceptEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_1",
                                              "message": "밴드 초대를 수락했습니다.",
                                              "result": {"bandId":1,"bandMemberProfileId":4,"nickname":"드럼천재","status":"ACCEPTED"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "MEMBER403_1", ref = "#/components/examples/MEMBER403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "MEMBER409_2", ref = "#/components/examples/MEMBER409_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandMemberAcceptResponse>> acceptInvite(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            @Valid BandMemberProfileCreateRequest request
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 초대 거절",
            description = "받은 초대를 거절합니다. 수락이 끝난 초대는 거절할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_4", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_4",
                                              "message": "밴드 초대를 거절했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_2", ref = "#/components/examples/BAND404_2")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND409_3", ref = "#/components/examples/BAND409_3")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> rejectInvite(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 초대 취소·멤버 제거",
            description = "밴드 오너가 대상 사용자의 초대를 취소하거나 멤버를 제거합니다. 경로에는 사용자 ID를 전달합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_2",
                                              "message": "밴드 멤버를 제거했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND403_1", ref = "#/components/examples/BAND403_1"),
                                    @ExampleObject(name = "BAND403_2", ref = "#/components/examples/BAND403_2")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1"),
                                    @ExampleObject(name = "BAND404_2", ref = "#/components/examples/BAND404_2")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> removeMember(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            @Parameter(example = "42", description = "초대를 취소하거나 제거할 대상의 사용자 ID") Long userId
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "초대 대상 닉네임 검색",
            description = "닉네임을 기준으로 대상을 검색하고 초대 가능 여부를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.InviteTargetsEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_5", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_5",
                                              "message": "검색 결과를 조회했습니다.",
                                              "result": [{"userId":2,"nickname":"테스트","bandMemberStatus":"ACCEPTED","inviteAvailable":false},{"userId":3,"nickname":"테스트","bandMemberStatus":"ACCEPTED","inviteAvailable":false},{"userId":4,"nickname":"테스트","bandMemberStatus":null,"inviteAvailable":true}],
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "MEMBER400_1", ref = "#/components/examples/MEMBER400_1"),
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<List<BandMemberSearchItem>>> searchInviteTargets(
            @Parameter(example = "1") Long bandId,
            @Parameter(example = "지훈") String keyword
    );
}
