package com.umc.bscene.domain.auth.controller.docs;

import com.umc.bscene.domain.auth.dto.auth.request.LogoutRequest;
import com.umc.bscene.domain.auth.dto.auth.request.ReissueRequest;
import com.umc.bscene.domain.auth.dto.auth.response.ReissueResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

public interface AuthApi {

    String REFRESH_TOKEN_REQUEST_EXAMPLE = """
            {
              "refreshToken": "refresh-token"
            }
            """;

    @Operation(tags = "인증/인가", summary = "로그아웃",
            description = "전달한 Refresh Token을 무효화합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LogoutRequest.class),
                            examples = @ExampleObject(value = REFRESH_TOKEN_REQUEST_EXAMPLE))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseSchemas.LogoutEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "AUTH200_5",
                                            value = """
                                            {"isSuccess":true,"status":200,"code":"AUTH200_5","message":"로그아웃되었습니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
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
            @ApiResponse(responseCode = "401", description = "인증 정보 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "AUTH401_4",
                                            value = """
                                            {"isSuccess":false,"status":401,"code":"AUTH401_4","message":"유효하지 않은 Refresh Token입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
                                            """)
                            }))
    })
    ResponseEntity<SuccessResponse<Void>> logout(@Valid LogoutRequest request);

    @Operation(tags = "인증/인가", summary = "Access Token 재발급",
            description = "유효한 Refresh Token을 사용해 Access Token과 새 Refresh Token을 발급합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReissueRequest.class),
                            examples = @ExampleObject(value = REFRESH_TOKEN_REQUEST_EXAMPLE))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseSchemas.ReissueEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "AUTH200_6",
                                            value = """
                                            {"isSuccess":true,"status":200,"code":"AUTH200_6","message":"토큰이 재발급되었습니다.","result":{"grantType":"Bearer","accessToken":"example-access-token","refreshToken":"example-refresh-token","accessTokenExpiresIn":3600000},"timeStamp":"2026-11-26 09:15:00"}
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
            @ApiResponse(responseCode = "401", description = "인증 정보 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "AUTH401_4",
                                            value = """
                                            {"isSuccess":false,"status":401,"code":"AUTH401_4","message":"유효하지 않은 Refresh Token입니다.","result":null,"timeStamp":"2026-11-26 09:15:00"}
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
    ResponseEntity<SuccessResponse<ReissueResponse>> reissue(@Valid ReissueRequest request);
}
