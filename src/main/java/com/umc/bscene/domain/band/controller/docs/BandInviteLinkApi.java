package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.request.BandInviteLinkCreateRequest;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkResponse;
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

public interface BandInviteLinkApi {

    @Operation(tags = "밴드모드_내밴드(밴드 프로필)", summary = "밴드 초대 링크 생성",
            description = "밴드 오너가 멤버 또는 세션용 초대 링크를 발급합니다. 유효한 기존 링크는 재사용합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandInviteLinkCreateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "memberType": "SESSION"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandApiResponseSchemas.InviteLinkEnvelope.class), examples = {
                                    @ExampleObject(name = "BAND201_3", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 201,
                                              "code": "BAND201_3",
                                              "message": "밴드 초대 링크를 발급했습니다.",
                                              "result": {"bandId":1,"memberType":"SESSION","token":"00000000-0000-4000-8000-000000000001","expiresAt":"2026-07-31T01:47:04.350279"},
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
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<BandInviteLinkResponse>> issueInviteLink(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            BandInviteLinkCreateRequest request
    );
}
