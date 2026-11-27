package com.umc.bscene.domain.oauth.controller.docs;

import com.umc.bscene.domain.auth.controller.docs.AuthResponseSchemas;
import com.umc.bscene.domain.oauth.dto.request.AppleChallengeRequest;
import com.umc.bscene.domain.oauth.dto.response.AppleChallengeResponse;
import com.umc.bscene.domain.oauth.dto.request.AppleMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

public interface AppleMobileOauthApi {

    @Operation(tags = "모바일 SDK", summary = "Apple 로그인 요청 발급",
            description = "Apple SDK 로그인 직전에 빈 JSON 객체를 보내 challenge ID와 nonce를 발급받습니다. nonce는 SDK 호출에 사용합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(value = "{}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseSchemas.AppleChallengeEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "OAUTH200_2",
                                            value = """
                                            {"isSuccess":true,"status":200,"code":"OAUTH200_2","message":"Apple 로그인 요청이 준비되었습니다.","result":{"challengeId":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","nonce":"example-nonce","expiresInSeconds":300},"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_2",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"COMMON_400_2","message":"HTTP 요청 바디의 형식이 잘못되었습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "429", description = "요청 횟수 초과",
                    headers = @Header(name = "Retry-After", description = "재시도까지 남은 초", schema = @Schema(type = "integer")),
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "OAUTH429_1",
                                            value = """
                                            {"isSuccess":false,"status":429,"code":"OAUTH429_1","message":"소셜 로그인 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "503", description = "인증 서비스 일시 사용 불가",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_503_1",
                                            value = """
                                            {"isSuccess":false,"status":503,"code":"COMMON_503_1","message":"인증 저장소를 일시적으로 사용할 수 없습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            }))
    })
    ResponseEntity<SuccessResponse<AppleChallengeResponse>> issueChallenge(AppleChallengeRequest request);

    @Operation(tags = "모바일 SDK", summary = "Apple 모바일 로그인",
            description = "Apple SDK가 같은 로그인에서 반환한 identityToken과 authorizationCode를 서버가 발급한 challengeId와 함께 전달합니다. "
                    + "카카오·Apple 모바일 로그인은 IP별 호출 제한을 공유합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppleMobileLoginRequest.class),
                            examples = @ExampleObject(value = "{\"identityToken\":\"example-apple-identity-token\",\"authorizationCode\":\"example-authorization-code\",\"challengeId\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"))))
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
                                    @ExampleObject(name = "OAUTH400_6",
                                            value = """
                                            {"isSuccess":false,"status":400,"code":"OAUTH400_6","message":"유효하지 않거나 만료된 Apple 로그인 요청입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "인증 정보 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "OAUTH401_1",
                                            value = """
                                            {"isSuccess":false,"status":401,"code":"OAUTH401_1","message":"유효하지 않거나 만료된 소셜 인증 정보입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "OAUTH401_3",
                                            value = """
                                            {"isSuccess":false,"status":401,"code":"OAUTH401_3","message":"Apple 로그인 요청 정보가 일치하지 않습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "409", description = "요청 상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "OAUTH409_3",
                                            value = """
                                            {"isSuccess":false,"status":409,"code":"OAUTH409_3","message":"이전 Apple 로그인 정보를 정리 중입니다. 완료 후 새 로그인으로 다시 시도해 주세요.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "429", description = "요청 횟수 초과",
                    headers = @Header(name = "Retry-After", description = "재시도까지 남은 초", schema = @Schema(type = "integer")),
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "OAUTH429_1",
                                            value = """
                                            {"isSuccess":false,"status":429,"code":"OAUTH429_1","message":"소셜 로그인 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            })),
            @ApiResponse(responseCode = "503", description = "인증 서비스 일시 사용 불가",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "OAUTH503_1",
                                            value = """
                                            {"isSuccess":false,"status":503,"code":"OAUTH503_1","message":"소셜 인증 서비스에 일시적으로 연결할 수 없습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """),
                                    @ExampleObject(name = "COMMON_503_1",
                                            value = """
                                            {"isSuccess":false,"status":503,"code":"COMMON_503_1","message":"인증 저장소를 일시적으로 사용할 수 없습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            }))
    })
    ResponseEntity<SuccessResponse<OauthLoginResponse>> login(AppleMobileLoginRequest request);
}
