package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.response.BandInviteLinkDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkEntryResponse;
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

public interface BandInviteLinkAccessApi {

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 초대 링크 조회",
            description = "초대 토큰으로 밴드와 초대 유형, 만료 정보를 조회합니다. 로그인 없이 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.InviteLinkDetailsEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND200_13", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_13",
                                              "message": "밴드 초대 링크 정보를 조회했습니다.",
                                              "result": {"bandId":1,"bandName":"OoF","bandProfileImageUrl":"https://bucket.s3.ap-northeast-2.amazonaws.com/band/apple.png","genre":"ETC","region":"SEOUL","memberType":"SESSION","expiresAt":"2026-07-31T01:47:04.350279"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND400_1", ref = "#/components/examples/BAND400_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_5", ref = "#/components/examples/BAND404_5")
                            }))
    })
    ResponseEntity<SuccessResponse<BandInviteLinkDetailResponse>> getInviteLink(
            @Parameter(example = "00000000-0000-4000-8000-000000000001") String token
    );

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 초대 링크 진입",
            description = "로그인한 사용자의 초대를 등록합니다. 멤버 편입은 등록 후 초대를 수락해야 완료됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.InviteLinkEntryEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND201_4", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 201,
                                              "code": "BAND201_4",
                                              "message": "초대 링크를 통한 밴드 초대를 등록했습니다.",
                                              "result": {"bandMemberId":7,"bandId":1,"memberType":"SESSION","status":"INVITED"},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND400_1", ref = "#/components/examples/BAND400_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_5", ref = "#/components/examples/BAND404_5")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "MEMBER409_1", ref = "#/components/examples/MEMBER409_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandInviteLinkEntryResponse>> enterInviteLink(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "00000000-0000-4000-8000-000000000001") String token
    );
}
