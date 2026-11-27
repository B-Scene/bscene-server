package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.request.BandOwnerTransferRequest;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import org.springframework.http.ResponseEntity;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

public interface MyBandApi {

    @Operation(tags = "마이", summary = "밴드 탈퇴",
            description = "밴드 멤버가 탈퇴합니다. 밴드 오너는 권한을 양도한 뒤 탈퇴할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "BAND200_14", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_14",
                                              "message": "밴드에서 탈퇴했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "BAND403_3", ref = "#/components/examples/BAND403_3"),
                                    @ExampleObject(name = "BAND403_5", ref = "#/components/examples/BAND403_5")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1"),
                                    @ExampleObject(name = "BAND404_2", ref = "#/components/examples/BAND404_2")
                            }))
    })
    ResponseEntity<SuccessResponse<Void>> leaveBand(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId
    );

    @Operation(tags = "마이", summary = "밴드 오너 양도",
            description = "해당 밴드의 오너만 권한을 양도할 수 있습니다. 양도 대상은 가입이 완료된 정식 멤버(MEMBER)여야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandOwnerTransferRequest.class),
                            examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "newOwnerUserId": 2
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "BAND200_15", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "BAND200_15",
                                              "message": "밴드 오너 권한을 양도했습니다.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                                    @ExampleObject(name = "BAND400_2", ref = "#/components/examples/BAND400_2"),
                                    @ExampleObject(name = "BAND400_3", ref = "#/components/examples/BAND400_3")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "BAND403_1", ref = "#/components/examples/BAND403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1"),
                                    @ExampleObject(name = "BAND404_2", ref = "#/components/examples/BAND404_2")
                            }))
    })
    ResponseEntity<SuccessResponse<Void>> transferOwnership(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            @Valid BandOwnerTransferRequest request
    );
}
