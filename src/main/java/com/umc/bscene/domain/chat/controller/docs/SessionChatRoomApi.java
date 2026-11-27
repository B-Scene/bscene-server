package com.umc.bscene.domain.chat.controller.docs;

import com.umc.bscene.domain.chat.dto.request.ChatRoomCreateRequest;
import com.umc.bscene.domain.chat.dto.response.ChatRoomCreateResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomDetailResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomListResponse;
import com.umc.bscene.domain.chat.enums.ChatRoomFilter;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import com.umc.bscene.global.swagger.SessionApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

public interface SessionChatRoomApi {

    @Operation(tags = "세션", summary = "쪽지방 생성 또는 조회",
            description = "모집 공고, 제출한 지원서 또는 세션 찾기 결과에서 쪽지방을 생성하거나 기존 방을 가져옵니다. contextType에 맞는 대상 ID 한 개를 보내세요.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChatRoomCreateRequest.class), examples = {
                            @ExampleObject(name = "모집 공고에서 시작", value = """
                                    {
                                      "contextType": "RECRUITMENT",
                                      "sessionRecruitmentId": 15
                                    }
                                    """),
                            @ExampleObject(name = "세션 찾기에서 시작", value = """
                                    {
                                      "contextType": "SESSION_SEARCH",
                                      "sessionApplicationId": 7
                                    }
                                    """),
                            @ExampleObject(name = "제출된 지원서에서 시작", value = """
                                    {
                                      "contextType": "RECRUITMENT",
                                      "applicationSubmissionId": 1
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "성공 (HTTP 200, 응답 본문 status 201)", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.ChatRoomCreateResponseEnvelope.class), examples = {
                            @ExampleObject(name = "새로 생성된 쪽지방", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "CHAT201_1",
                                      "message": "채팅방 생성에 성공했습니다.",
                                      "result": {
                                        "chatRoomId": 3,
                                        "contextType": "RECRUITMENT",
                                        "contextId": 15,
                                        "title": "드럼 세션 구합니다",
                                        "genre": "인디",
                                        "part": "드럼",
                                        "recipientUserId": 2,
                                        "recipientName": "WAVY",
                                        "created": true
                                      },
                                      "timeStamp": "2026-07-12 21:40:00"
                                    }
                                    """),
                            @ExampleObject(name = "기존 쪽지방", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "CHAT201_1",
                                      "message": "채팅방 생성에 성공했습니다.",
                                      "result": {
                                        "chatRoomId": 4,
                                        "contextType": "RECRUITMENT",
                                        "contextId": 15,
                                        "title": "드럼 세션 모집",
                                        "genre": "인디",
                                        "part": "드럼",
                                        "recipientUserId": 1,
                                        "recipientName": "APPLEs",
                                        "created": false
                                      },
                                      "timeStamp": "2026-07-22 15:35:58"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "CHAT400_1", ref = "#/components/examples/CHAT400_1"),
                            @ExampleObject(name = "CHAT400_2", ref = "#/components/examples/CHAT400_2"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "CHAT403_2", ref = "#/components/examples/CHAT403_2"),
                            @ExampleObject(name = "CHAT403_3", ref = "#/components/examples/CHAT403_3")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "CHAT404_1", ref = "#/components/examples/CHAT404_1")
                    }))
    })
    SuccessResponse<ChatRoomCreateResponse> createChatRoom(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid ChatRoomCreateRequest request
    );

    @Operation(tags = "세션", summary = "쪽지함 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "쪽지함 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.ChatRoomListResponseEnvelope.class), examples = {
                            @ExampleObject(name = "데이터 있음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "CHAT200_1",
                                      "message": "쪽지함 조회에 성공했습니다.",
                                      "result": {
                                        "content": [
                                          {
                                            "chatRoomId": 1,
                                            "contextType": "RECRUITMENT",
                                            "contextId": 15,
                                            "counterpartUserId": 2,
                                            "counterpartName": "정하람",
                                            "counterpartProfileImageUrl": "https://example.com/session-profile.jpg",
                                            "applicationStatus": null,
                                            "lastMessage": "드럼 세션 지원 관련해서 연락드려요.",
                                            "lastMessageAt": "2026-07-22T10:00:00",
                                            "unreadCount": 2,
                                            "canSend": true
                                          },
                                          {
                                            "chatRoomId": 2,
                                            "contextType": "RECRUITMENT",
                                            "contextId": 16,
                                            "counterpartUserId": 3,
                                            "counterpartName": "이지수",
                                            "counterpartProfileImageUrl": "https://example.com/profile2.jpg",
                                            "applicationStatus": "수락",
                                            "lastMessage": "앞으로 잘 부탁드립니다.",
                                            "lastMessageAt": "2026-07-22T09:30:00",
                                            "unreadCount": 0,
                                            "canSend": true
                                          },
                                          {
                                            "chatRoomId": 3,
                                            "contextType": "RECRUITMENT",
                                            "contextId": 17,
                                            "counterpartUserId": 4,
                                            "counterpartName": "김민수",
                                            "counterpartProfileImageUrl": null,
                                            "applicationStatus": "거절",
                                            "lastMessage": "지원해주셔서 감사합니다.",
                                            "lastMessageAt": "2026-07-22T08:20:00",
                                            "unreadCount": 0,
                                            "canSend": false
                                          }
                                        ],
                                        "size": 20,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-22 10:10:00"
                                    }
                                    """),
                            @ExampleObject(name = "데이터 없음", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "CHAT200_1",
                                      "message": "쪽지함 조회에 성공했습니다.",
                                      "result": {
                                        "content": [],
                                        "size": 20,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-22 10:10:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    SuccessResponse<ChatRoomListResponse> getChatRooms(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "쪽지함 필터", example = "ALL") ChatRoomFilter filter,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지에서는 생략") Long cursorId,
            @Parameter(description = "페이지 크기") Integer size
    );

    @Operation(tags = "세션", summary = "쪽지방 상세 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "쪽지 상세 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = SessionApiResponseSchemas.ChatRoomDetailResponseEnvelope.class), examples = {
                            @ExampleObject(name = "세션 찾기 쪽지방", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "CHAT200_2",
                                      "message": "쪽지 상세 조회에 성공했습니다.",
                                      "result": {
                                        "chatRoomId": 1,
                                        "contextType": "SESSION_SEARCH",
                                        "sessionApplicationId": 14,
                                        "sessionRecruitmentId": null,
                                        "applicationSubmissionId": null,
                                        "opponentUserId": 3,
                                        "opponentName": "정하람",
                                        "opponentProfileImageUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/session_profile/profile.png",
                                        "canSend": true,
                                        "messages": [
                                          {
                                            "chatMessageId": 1,
                                            "senderUserId": 3,
                                            "senderName": "정하람",
                                            "content": "혹시 아직 드럼 세션 구하시나요?",
                                            "isMine": false,
                                            "isRead": true,
                                            "createdAt": "2026-06-02 10:00:00"
                                          },
                                          {
                                            "chatMessageId": 2,
                                            "senderUserId": 1,
                                            "senderName": "나",
                                            "content": "네, 아직 모집 중입니다.",
                                            "isMine": true,
                                            "isRead": true,
                                            "createdAt": "2026-06-02 10:03:00"
                                          },
                                          {
                                            "chatMessageId": 3,
                                            "senderUserId": 3,
                                            "senderName": "정하람",
                                            "content": "감사합니다!",
                                            "isMine": false,
                                            "isRead": true,
                                            "createdAt": "2026-06-02 10:05:00"
                                          }
                                        ],
                                        "size": 20,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-13 02:10:00"
                                    }
                                    """),
                            @ExampleObject(name = "모집 공고 쪽지방", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "CHAT200_2",
                                      "message": "쪽지 상세 조회에 성공했습니다.",
                                      "result": {
                                        "chatRoomId": 2,
                                        "contextType": "RECRUITMENT",
                                        "sessionApplicationId": 7,
                                        "sessionRecruitmentId": 8,
                                        "applicationSubmissionId": 12,
                                        "opponentUserId": 5,
                                        "opponentName": "apple Band",
                                        "opponentProfileImageUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/band_profile/apple.png",
                                        "canSend": true,
                                        "messages": [
                                          {
                                            "chatMessageId": 4,
                                            "senderUserId": 1,
                                            "senderName": "나",
                                            "content": "공고 보고 연락드립니다.",
                                            "isMine": true,
                                            "isRead": true,
                                            "createdAt": "2026-07-12 15:00:00"
                                          },
                                          {
                                            "chatMessageId": 5,
                                            "senderUserId": 5,
                                            "senderName": "밴드 오너",
                                            "content": "안녕하세요. 지원서 확인했습니다.",
                                            "isMine": false,
                                            "isRead": true,
                                            "createdAt": "2026-07-12 15:10:00"
                                          }
                                        ],
                                        "size": 20,
                                        "nextCursor": null,
                                        "hasNext": false
                                      },
                                      "timeStamp": "2026-07-13 02:10:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "CHAT403_3", ref = "#/components/examples/CHAT403_3")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "CHAT404_2", ref = "#/components/examples/CHAT404_2")
                    }))
    })
    SuccessResponse<ChatRoomDetailResponse> getChatRoomDetail(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "쪽지방 ID", example = "1") Long chatRoomId,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지에서는 생략") Long cursorId,
            @Parameter(description = "페이지 크기") Integer size
    );

    @Operation(tags = "세션", summary = "쪽지방 나가기",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "채팅방 나가기에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "CHAT200_3",
                                      "message": "채팅방 나가기에 성공했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-13 14:47:52"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "CHAT403_3", ref = "#/components/examples/CHAT403_3")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "CHAT404_2", ref = "#/components/examples/CHAT404_2")
                    }))
    })
    SuccessResponse<Void> leaveChatRoom(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "쪽지방 ID", example = "1") Long chatRoomId
    );
}
