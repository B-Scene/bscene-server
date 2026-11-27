package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.chat.enums.code.error.ChatErrorCode;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Spring MVC 자동 탐색에 포함되지 않는 실제 WebSocket 핸드셰이크를 문서화한다. */
public final class WebSocketOpenApi {

    public static final String TAG = "웹소켓";

    private static final String LIVE_CHAT = """
            WebSocket 클라이언트에서 `wss://api.bscene.app/api/ws/lives/{liveId}/chat?ticket={ticket}`에 연결하고 서브프로토콜로 `live-chat.v1`을 지정합니다. 로컬은 `ws://localhost:8080/api/ws/lives/{liveId}/chat?ticket={ticket}`을 사용합니다.
            
            티켓은 연결 검증에서 한 번 소비됩니다. 재연결할 때는 대응하는 티켓 발급 API를 다시 호출하세요. Swagger는 연결과 프레임 형식을 표시합니다. 실제 연결은 WebSocket 클라이언트에서 실행하세요.
            
            티켓 발급과 연결 주소의 `liveId`가 같아야 합니다. 메시지는 같은 라이브의 연결에 전달됩니다. 차단 관계에 있거나 해당 라이브에서 발신자를 신고한 사용자는 그 메시지를 받지 않습니다.
            
            ### 전송
            
            `clientMsgId`는 메시지마다 새 UUID로 생성합니다. 본문은 앞뒤 공백 제거 후 1~500자를 허용합니다.
            
            `live-chat.send`
            
            ```json
            {"type":"live-chat.send","data":{"content":"메시지"},"clientMsgId":"550e8400-e29b-41d4-a716-446655440000"}
            ```
            
            `ping`
            
            ```json
            {"type":"ping","data":{},"clientMsgId":null}
            ```
            
            ### 수신
            
            `senderProfileImageUrl`은 발신자의 팬 프로필이나 방송 밴드 프로필에 따라 URL 또는 `null`이 됩니다. 발신자는 `clientMsgId`로 서버 프레임과 임시 메시지를 맞춥니다.
            
            `system.event · connected`
            
            ```json
            {"type":"system.event","id":null,"data":{"event":"connected"},"clientMsgId":null,"timeStamp":"2026-07-22 20:27:42"}
            ```
            
            `live-chat.message`
            
            ```json
            {"type":"live-chat.message","id":null,"data":{"messageId":"949c3613-4b1b-4bfe-b238-e8d04685baf8","liveId":1,"senderId":4,"senderName":"홍길동","senderProfileImageUrl":"https://cdn.bscene.com/fans/4/profile.png","content":"메시지","sentAt":"2026-07-22 20:30:16"},"clientMsgId":"550e8400-e29b-41d4-a716-446655440000","timeStamp":"2026-07-22 20:30:16"}
            ```
            
            `pong`
            
            ```json
            {"type":"pong","id":null,"data":{},"clientMsgId":null,"timeStamp":"2026-07-22 20:30:17"}
            ```
            
            `system.event · live-ended`
            
            ```json
            {"type":"system.event","id":null,"data":{"event":"live-ended"},"clientMsgId":null,"timeStamp":"2026-07-22 20:35:00"}
            ```
            
            ### 메시지 처리 오류
            
            연결 중 오류는 `system.error` 프레임으로 전달됩니다.
            
            ```json
            {"type":"system.error","id":null,"data":{"code":"LIVE400_6","message":"채팅 내용을 입력해주세요."},"clientMsgId":"550e8400-e29b-41d4-a716-446655440000","timeStamp":"2026-07-22 20:30:18"}
            ```
            """;

    private static final String DM = """
            WebSocket 클라이언트에서 `wss://api.bscene.app/api/ws/chat?ticket={ticket}`에 연결하고 서브프로토콜로 `dm.v1`을 지정합니다. 로컬은 `ws://localhost:8080/api/ws/chat?ticket={ticket}`을 사용합니다.
            
            티켓은 연결 검증에서 한 번 소비됩니다. 재연결할 때는 대응하는 티켓 발급 API를 다시 호출하세요. Swagger는 연결과 프레임 형식을 표시합니다. 실제 연결은 WebSocket 클라이언트에서 실행하세요.
            
            로그인 후 앱 단위로 연결합니다. 연결되지 않은 동안 받은 쪽지는 기존 쪽지 상세 조회 API로 확인하세요. 메시지는 저장 후 발신자와 수신자에게 전달되며 수신자 프레임의 `clientMsgId`는 `null`입니다.
            
            ### 전송
            
            `dm.send`는 새 UUID를 사용하고 본문은 앞뒤 공백 제거 후 1~2,000자를 허용합니다. `dm.read`는 마지막으로 읽은 메시지 ID를 전달하고 `clientMsgId`는 `null`로 보냅니다.
            
            `dm.send`
            
            ```json
            {"type":"dm.send","data":{"chatRoomId":4,"content":"메시지"},"clientMsgId":"550e8400-e29b-41d4-a716-446655440000"}
            ```
            
            `dm.read`
            
            ```json
            {"type":"dm.read","data":{"chatRoomId":4,"lastReadMessageId":36},"clientMsgId":null}
            ```
            
            `ping`
            
            ```json
            {"type":"ping","data":{},"clientMsgId":null}
            ```
            
            ### 수신
            
            `dm.message`의 `chatMessageId`를 기준으로 중복 표시를 막을 수 있습니다.
            
            `system.event · connected`
            
            ```json
            {"type":"system.event","id":null,"data":{"event":"connected"},"clientMsgId":null,"timeStamp":"2026-07-22 20:27:42"}
            ```
            
            `dm.message`
            
            ```json
            {"type":"dm.message","id":36,"data":{"chatMessageId":36,"chatRoomId":4,"senderId":1,"senderName":"홍길동","profileImageUrl":"https://example.com/session-profile.jpg","content":"메시지","readAt":null,"createdAt":"2026-07-22 16:20:34"},"clientMsgId":"550e8400-e29b-41d4-a716-446655440000","timeStamp":"2026-07-22 16:20:34"}
            ```
            
            `dm.read`
            
            ```json
            {"type":"dm.read","id":null,"data":{"chatRoomId":4,"readerId":4,"lastReadMessageId":36,"readAt":"2026-07-22 16:22:16"},"clientMsgId":null,"timeStamp":"2026-07-22 16:22:16"}
            ```
            
            `pong`
            
            ```json
            {"type":"pong","id":null,"data":{},"clientMsgId":null,"timeStamp":"2026-07-22 20:30:17"}
            ```
            
            ### 메시지 처리 오류
            
            연결 중 오류는 `system.error` 프레임으로 전달됩니다.
            
            ```json
            {"type":"system.error","id":null,"data":{"code":"CHAT403_5","message":"현재 이 채팅방에 쪽지를 보낼 수 없습니다."},"clientMsgId":"550e8400-e29b-41d4-a716-446655440000","timeStamp":"2026-07-22 16:20:36"}
            ```
            """;

    private WebSocketOpenApi() {
    }

    public static void addHandshakes(Paths paths) {
        Operation live = handshake("connectLiveChat", "라이브 채팅 연결 및 메시지", "live-chat.v1",
                LIVE_CHAT + errorTable(ChatErrorCode.LIVE_INVALID_FRAME, ChatErrorCode.LIVE_EMPTY_CONTENT,
                        ChatErrorCode.LIVE_CONTENT_TOO_LONG, ChatErrorCode.LIVE_UNSUPPORTED_TYPE,
                        ChatErrorCode.LIVE_INTERNAL_ERROR), "https://app.notion.com/p/425e776b82e380c5a495d8094463e16d",
                "티켓이 누락·만료·소비되었거나 티켓의 라이브 ID와 연결 경로의 라이브 ID가 다름");
        live.addParametersItem(new PathParameter().name("liveId").required(true)
                .description("티켓 발급에 사용한 라이브 ID").schema(new IntegerSchema().format("int64")));
        paths.addPathItem("/ws/lives/{liveId}/chat", new PathItem().get(live));

        paths.addPathItem("/ws/chat", new PathItem().get(handshake("connectDirectMessages",
                "쪽지 연결 및 메시지", "dm.v1", DM + errorTable(ChatErrorCode.DM_INVALID_FRAME,
                        ChatErrorCode.DM_EMPTY_CONTENT, ChatErrorCode.DM_CONTENT_TOO_LONG,
                        ChatErrorCode.DM_UNSUPPORTED_TYPE, ChatErrorCode.CHAT_ROOM_ACCESS_DENIED,
                        ChatErrorCode.DM_USER_BLOCKED, ChatErrorCode.DM_SEND_NOT_ALLOWED,
                        ChatErrorCode.CHAT_ROOM_NOT_FOUND, ChatErrorCode.DM_READ_MESSAGE_NOT_FOUND,
                        ChatErrorCode.DM_INTERNAL_ERROR), "https://app.notion.com/p/425e776b82e38026b67df0472059bda1",
                "티켓이 누락·만료·소비되었거나 새 티켓 발급으로 무효화됨")));
    }

    private static Operation handshake(String operationId, String summary, String protocol,
                                       String description, String notionUrl, String unauthorized) {
        return new Operation().operationId(operationId).summary(summary).description(description)
                .addTagsItem(TAG).externalDocs(new ExternalDocumentation().description("Notion 웹소켓 명세").url(notionUrl))
                .addParametersItem(new QueryParameter().name("ticket").required(true)
                        .description("티켓 발급 API의 result.ticket").schema(new StringSchema()))
                .addParametersItem(new HeaderParameter().name("Sec-WebSocket-Protocol").required(true)
                        .description("WebSocket 연결 시 지정할 서브프로토콜")
                        .schema(new StringSchema()._enum(List.of(protocol))))
                .responses(new ApiResponses()
                        .addApiResponse("101", new ApiResponse().description("WebSocket 연결 성공")
                                .addHeaderObject("Sec-WebSocket-Protocol", new Header()
                                        .description("협상된 서브프로토콜")
                                        .schema(new StringSchema().example(protocol))))
                        .addApiResponse("400", new ApiResponse().description("서브프로토콜이 없거나 올바르지 않은 요청"))
                        .addApiResponse("401", new ApiResponse().description(unauthorized))
                        .addApiResponse("403", new ApiResponse().description("허용되지 않은 Origin")));
    }

    private static String errorTable(ChatErrorCode... errors) {
        return Arrays.stream(errors)
                .map(error -> "| `" + error.getCode() + "` | " + error.getMessage() + " |")
                .collect(Collectors.joining("\n", "\n\n| 코드 | 메시지 |\n| --- | --- |\n", "\n"));
    }
}
