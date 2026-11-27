package com.umc.bscene.domain.chat.controller.docs;

import com.umc.bscene.domain.chat.dto.response.LiveChatWebSocketTicketResponse;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.SwaggerResponseEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public interface LiveChatWebSocketTicketApi {

    @Operation(tags = "웹소켓", summary = "라이브 채팅 연결 티켓 발급",
            description = "진행 중인 라이브의 연결 티켓을 발급합니다. 유효시간은 30초이며 연결 검증에서 한 번만 사용됩니다. 연결과 티켓 발급에 같은 liveId를 사용하세요.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 채팅 연결 티켓 발급에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = LiveChatTicketEnvelope.class),
                    examples = @ExampleObject(name = "티켓 발급 성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_16",
                                      "message": "라이브 채팅 연결 티켓 발급에 성공했습니다.",
                                      "result": {
                                        "ticket": "dVIcCYu0V5unbD7zgUpBpGXFdk8dI5xPJoMFz_MoqUE",
                                        "subprotocol": "live-chat.v1",
                                        "expiresInSeconds": 30
                                      },
                                      "timeStamp": "2026-07-22 16:12:42"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "400", description = "라이브 ID 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3"))),
            @ApiResponse(responseCode = "404", description = "방송 중인 라이브 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "LIVE404_6", ref = "#/components/examples/LIVE_CHAT_NOT_OPEN")))
    })
    SuccessResponse<LiveChatWebSocketTicketResponse> issueTicket(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "방송 중인 라이브 ID", example = "1") Long liveId
    );

    abstract class LiveChatTicketEnvelope extends SwaggerResponseEnvelope<LiveChatWebSocketTicketResponse> {
    }
}
