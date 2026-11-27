package com.umc.bscene.domain.session.controller.docs;

import com.umc.bscene.domain.session.dto.profile.request.SessionNameUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.request.SessionProfileImageUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.SessionBasicProfileResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionNameUpdateResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionProfileImageUpdateResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.SessionApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public interface SessionBasicProfileApi {

    @Operation(tags = "세션", summary = "세션 기본 정보 조회",
            description = "로그인한 사용자의 세션 기본 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "세션 기본 정보 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionBasicProfileResponseEnvelope.class), examples = {
                            @ExampleObject(name = "이메일과 사진 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_1",
                                      "message": "세션 기본 정보 조회에 성공했습니다.",
                                      "result": {
                                        "userId": 1,
                                        "name": "홍길동",
                                        "email": "haramdrums@example.com",
                                        "gender": "MALE",
                                        "birthDate": "1998-06-02",
                                        "profileImageUrl": "https://example.com/session-profile.jpg"
                                      },
                                      "timeStamp": "2026-07-21 20:32:49"
                                    }
                                    """),
                            @ExampleObject(name = "이메일과 사진 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_1",
                                      "message": "세션 기본 정보 조회에 성공했습니다.",
                                      "result": {
                                        "userId": 1,
                                        "name": "정하람",
                                        "email": null,
                                        "gender": "MALE",
                                        "birthDate": "1998-06-02",
                                        "profileImageUrl": null
                                      },
                                      "timeStamp": "2026-07-11 12:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<SessionBasicProfileResponse> getProfile(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "세션", summary = "세션 이름 수정",
            description = "세션 화면에 표시되는 이름을 수정합니다. 앞뒤 공백을 제거한 이름을 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SessionNameUpdateRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "name": "김희진"
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "세션 이름 수정에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionNameUpdateResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_25",
                                      "message": "세션 이름 수정에 성공했습니다.",
                                      "result": {
                                        "name": "김희진"
                                      },
                                      "timeStamp": "2026-11-26 09:15:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION400_10", ref = "#/components/examples/SESSION400_10"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_500_1", ref = "#/components/examples/COMMON_500_1")
                    }))
    })
    SuccessResponse<SessionNameUpdateResponse> updateName(
            @Parameter(hidden = true) AuthMember authMember,
            SessionNameUpdateRequest request
    );

    @Operation(tags = "세션", summary = "세션 프로필 사진 수정",
            description = "HTTP 또는 HTTPS 이미지 URL을 저장합니다. 빈 문자열을 보내면 현재 사진을 삭제합니다. GIF는 사용할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SessionProfileImageUpdateRequest.class), examples = {
                            @ExampleObject(name = "사진 저장", value = """
                                    {
                                      "profileImageUrl": "https://cdn.bscene.com/session/profile.png"
                                    }
                                    """),
                            @ExampleObject(name = "사진 삭제", value = """
                                    {
                                      "profileImageUrl": ""
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "세션 프로필 사진 수정에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.SessionProfileImageUpdateResponseEnvelope.class), examples = {
                            @ExampleObject(name = "사진 저장", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_26",
                                      "message": "세션 프로필 사진 수정에 성공했습니다.",
                                      "result": {
                                        "profileImageUrl": "https://cdn.bscene.com/session/profile.png"
                                      },
                                      "timeStamp": "2026-11-26 09:15:00"
                                    }
                                    """),
                            @ExampleObject(name = "사진 삭제", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "SESSION200_26",
                                      "message": "세션 프로필 사진 수정에 성공했습니다.",
                                      "result": {
                                        "profileImageUrl": null
                                      },
                                      "timeStamp": "2026-11-26 09:15:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "SESSION400_11", ref = "#/components/examples/SESSION400_11"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_500_1", ref = "#/components/examples/COMMON_500_1")
                    }))
    })
    SuccessResponse<SessionProfileImageUpdateResponse> updateProfileImage(
            @Parameter(hidden = true) AuthMember authMember,
            SessionProfileImageUpdateRequest request
    );
}
