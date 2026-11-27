package com.umc.bscene.domain.chat.controller.docs;

import com.umc.bscene.domain.chat.dto.response.ChatWebSocketTicketResponse;
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

public interface ChatWebSocketTicketApi {

    @Operation(tags = "웹소켓", summary = "쪽지 연결 티켓 발급",
            description = "쪽지 앱 연결용 티켓을 발급합니다. 유효시간은 30초이며 연결 검증에서 한 번만 사용됩니다. 같은 사용자가 새 티켓을 발급받으면 이전 미사용 티켓은 무효화됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "쪽지 웹소켓 연결 티켓 발급에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = DmTicketEnvelope.class),
                    examples = @ExampleObject(name = "티켓 발급 성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "CHAT200_4",
                                      "message": "쪽지 웹소켓 연결 티켓 발급에 성공했습니다.",
                                      "result": {
                                        "ticket": "cU9KbGO6i0t3dTgxSpD_sVTTW4NY1T72ylE7NFj5WIE",
                                        "subprotocol": "dm.v1",
                                        "expiresIn": 30
                                      },
                                      "timeStamp": "2026-07-22 16:12:42"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<ChatWebSocketTicketResponse> issueTicket(
            @Parameter(hidden = true) AuthMember authMember
    );

    abstract class DmTicketEnvelope extends SwaggerResponseEnvelope<ChatWebSocketTicketResponse> {
    }
}
