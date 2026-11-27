package com.umc.bscene.domain.oauth.controller.docs;

import com.umc.bscene.domain.auth.controller.docs.AuthResponseSchemas;
import com.umc.bscene.domain.oauth.dto.request.OauthExchangeRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.dto.request.OauthSignupRequest;
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

public interface OauthApi {

    @Operation(tags = "인증/인가", summary = "소셜 로그인 코드 교환",
            description = "카카오 웹 로그인 콜백에서 받은 60초 유효 일회용 코드를 로그인 결과로 교환합니다. 신규 회원에게는 가입 토큰을, 기존 회원에게는 로그인 토큰을 반환합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OauthExchangeRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "f2e650f6-d9a9-4005-89fd-88111abe5c35"
                                    }
                                    """))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseSchemas.OauthLoginEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "기존 회원",
                                            value = """
                                            {"isSuccess":true,"status":200,"code":"OAUTH200_1","message":"소셜 로그인에 성공했습니다.","result":{"isNewUser":false,"signupToken":null,"email":null,"token":{"grantType":"Bearer","accessToken":"example-access-token","refreshToken":"example-refresh-token","accessTokenExpiresIn":3600000,"user":{"userId":1,"name":"홍길동","currentMode":null,"onboardingCompleted":false}}},"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "신규 회원",
                                            value = """
                                            {"isSuccess":true,"status":200,"code":"OAUTH200_1","message":"소셜 로그인에 성공했습니다.","result":{"isNewUser":true,"signupToken":"example-signup-token","email":"user@example.com","token":null},"timeStamp":"2026-11-26 09:15:00"}
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
                                    @ExampleObject(name = "OAUTH400_5",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"OAUTH400_5","message":"유효하지 않거나 만료된 로그인 코드입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            }))
    })
    ResponseEntity<SuccessResponse<OauthLoginResponse>> exchange(OauthExchangeRequest request);

    @Operation(tags = "인증/인가", summary = "소셜 회원가입",
            description = "가입 토큰으로 소셜 회원가입을 완료합니다. 필수 약관은 모두 동의해야 하며 같은 약관 ID는 중복할 수 없습니다. 만 14세 이상부터 가입할 수 있습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OauthSignupRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "signupToken": "...",
                                      "name": "한수아",
                                      "birthDatePrefix": "000517",
                                      "genderCode": "3",
                                      "terms": [
                                        { "termId": 1, "agreed": true },
                                        { "termId": 2, "agreed": true },
                                        { "termId": 3, "agreed": true },
                                        { "termId": 4, "agreed": false }
                                      ]
                                    }
                                    """))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseSchemas.LoginOrSignupEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "OAUTH201_1",
                                            value = """
                                            {"isSuccess":true,"status":201,"code":"OAUTH201_1","message":"소셜 회원가입에 성공했습니다.","result":{"grantType":"Bearer","accessToken":"example-access-token","refreshToken":"example-refresh-token","accessTokenExpiresIn":3600000,"user":{"userId":1,"name":"홍길동","currentMode":null,"onboardingCompleted":false}},"timeStamp":"2026-11-26 09:15:00"}
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
                                    @ExampleObject(name = "OAUTH400_1",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"OAUTH400_1","message":"유효하지 않은 회원가입 토큰입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "OAUTH400_2",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"OAUTH400_2","message":"지원하지 않는 소셜 로그인입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "OAUTH400_3",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"OAUTH400_3","message":"생년월일 또는 성별 정보가 올바르지 않습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "OAUTH400_7",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"OAUTH400_7","message":"필수 약관에 모두 동의해야 합니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "OAUTH400_8",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"OAUTH400_8","message":"약관 정보가 올바르지 않습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "OAUTH400_9",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"OAUTH400_9","message":"만 14세 미만은 가입할 수 없습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "409", description = "요청 상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "OAUTH409_1",
                                            value = """
                                            {"isSuccess":false,"status":409,"code":"OAUTH409_1","message":"이미 가입된 소셜 계정입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            }))
    })
    ResponseEntity<SuccessResponse<TokenResponse>> signup(OauthSignupRequest request);
}
