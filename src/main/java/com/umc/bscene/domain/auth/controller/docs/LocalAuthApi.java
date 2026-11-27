package com.umc.bscene.domain.auth.controller.docs;

import com.umc.bscene.domain.auth.dto.auth.request.LoginRequest;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

public interface LocalAuthApi {

    @Operation(tags = "인증/인가", summary = "로컬 로그인",
            description = "dev/qa에서 기존 로컬 계정으로 로그인합니다. 운영 환경에서는 제공하지 않습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "loginId": "bscene123@example.com",
                                      "password": "Password123!"
                                    }
                                    """))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseSchemas.LoginOrSignupEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "AUTH200_2",
                                            value = """
                                            {"isSuccess":true,"status":200,"code":"AUTH200_2","message":"로그인에 성공했습니다.","result":{"grantType":"Bearer","accessToken":"example-access-token","refreshToken":"example-refresh-token","accessTokenExpiresIn":3600000,"user":{"userId":1,"name":"홍길동","currentMode":null,"onboardingCompleted":false}},"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_2",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"COMMON_400_2","message":"HTTP 요청 바디의 형식이 잘못되었습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "AUTH400_3",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"AUTH400_3","message":"아이디 또는 비밀번호가 올바르지 않습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "403", description = "사용할 수 없는 계정",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "AUTH403_1",
                                            value = """
                                            {"isSuccess":false,"status":403,"code":"AUTH403_1","message":"정지된 계정입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "AUTH403_2",
                                            value = """
                                            {"isSuccess":false,"status":403,"code":"AUTH403_2","message":"휴면 계정입니다. 휴면 해제가 필요합니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "AUTH403_3",
                                            value = """
                                            {"isSuccess":false,"status":403,"code":"AUTH403_3","message":"탈퇴한 계정입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            }))
    })
    ResponseEntity<SuccessResponse<TokenResponse>> login(LoginRequest request);
}
