package com.umc.bscene.domain.session.controller.docs;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentInterestResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.SessionApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

public interface SessionRecruitmentInterestApi {

    @Operation(tags = "세션", summary = "공고 찜하기",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 모집 공고 찜에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionRecruitmentInterestResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_14",
                                      "message": "세션 모집 공고 찜에 성공했습니다.",
                                      "result": {
                                        "sessionRecruitmentId": 3,
                                        "isInterested": true
                                      },
                                      "timeStamp": "2026-07-21 16:47:34"
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
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION409_1", ref = "#/components/examples/SESSION409_1")
                    }))
    })
    ResponseEntity<SuccessResponse<SessionRecruitmentInterestResponse>> setInterest(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "모집 공고 ID", example = "1") Long sessionRecruitmentId
    );

    @Operation(tags = "세션", summary = "공고 찜 취소",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 모집 공고 찜 취소에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionRecruitmentInterestResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_15",
                                      "message": "세션 모집 공고 찜 취소에 성공했습니다.",
                                      "result": {
                                        "sessionRecruitmentId": 3,
                                        "isInterested": false
                                      },
                                      "timeStamp": "2026-07-21 16:47:50"
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
    ResponseEntity<SuccessResponse<SessionRecruitmentInterestResponse>> unsetInterest(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "모집 공고 ID", example = "1") Long sessionRecruitmentId
    );
}
