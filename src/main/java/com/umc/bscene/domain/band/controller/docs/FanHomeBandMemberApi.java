package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.response.BandPublicMemberProfileResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.FanHomeApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface FanHomeBandMemberApi {

    @Operation(tags = "팬모드_홈", summary = "밴드 구성원 프로필 목록 조회",
            description = "밴드에서 공개된 정식 구성원의 닉네임, 파트, 오너 여부를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "밴드 구성원 프로필을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanHomeApiResponseSchemas.BandPublicMemberProfileListEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "BAND200_11",
                                      "message": "밴드 구성원 프로필을 조회했습니다.",
                                      "result": [
                                        {
                                          "bandMemberId": 1,
                                          "nickname": "영",
                                          "part": "드럼",
                                          "owner": true
                                        },
                                        {
                                          "bandMemberId": 2,
                                          "nickname": "테스트",
                                          "part": "기타",
                                          "owner": false
                                        },
                                        {
                                          "bandMemberId": 3,
                                          "nickname": "테스트1",
                                          "part": "보컬",
                                          "owner": false
                                        }
                                      ],
                                      "timeStamp": "2026-07-18 01:35:12"
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
    ResponseEntity<SuccessResponse<List<BandPublicMemberProfileResponse>>> getPublicMemberProfiles(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "밴드 ID", example = "1") Long bandId
    );
}
