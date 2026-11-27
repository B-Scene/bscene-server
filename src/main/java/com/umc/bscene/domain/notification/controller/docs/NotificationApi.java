package com.umc.bscene.domain.notification.controller.docs;

import com.umc.bscene.domain.notification.dto.request.PushTestSendRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenDeleteRequest;
import com.umc.bscene.domain.notification.dto.request.PushTokenSaveRequest;
import com.umc.bscene.domain.notification.dto.response.NotificationListItemResponse;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import com.umc.bscene.global.swagger.NotificationApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;

public interface NotificationApi {

    @Operation(tags = "알림", summary = "푸시 알림 토큰 등록/갱신",
            description = "현재 기기의 FCM 토큰을 등록하거나 갱신합니다. 지원 플랫폼은 WEB입니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PushTokenSaveRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "token": "test-fcm-token-001",
                                      "platform": "WEB"
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "푸시 알림 토큰이 저장되었습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_1",
                                      "message": "푸시 알림 토큰이 저장되었습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-09 14:30:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> savePushToken(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid PushTokenSaveRequest request
    );

    @Operation(tags = "알림", summary = "푸시 알림 토큰 삭제",
            description = "현재 계정에 등록된 지정 FCM 토큰을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PushTokenDeleteRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "token": "test-fcm-token-001"
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "푸시 알림 토큰이 삭제되었습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_2",
                                      "message": "푸시 알림 토큰이 삭제되었습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-09 14:30:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> deletePushToken(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid PushTokenDeleteRequest request
    );

    @Operation(tags = "알림", summary = "푸시 알림 테스트",
            description = "내 계정에 등록된 기기로 테스트 푸시를 발송합니다. 등록 토큰이 없으면 발송 없이 성공으로 응답합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PushTestSendRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "title": "테스트 알림",
                                      "body": "FCM 테스트 메시지입니다."
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "테스트 푸시 알림을 발송했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_3",
                                      "message": "테스트 푸시 알림을 발송했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-09 14:30:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "502", description = "푸시 발송 실패", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "NOTIFICATION502_1", ref = "#/components/examples/NOTIFICATION502_1")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> sendTestPush(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid PushTestSendRequest request
    );

    @Operation(tags = "알림", summary = "알림 읽음 처리",
            description = "내 알림을 읽음으로 표시합니다. 이미 읽은 경우 기존 읽음 시각을 유지합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "알림을 읽음 처리했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_5",
                                      "message": "알림을 읽음 처리했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-07-18 14:30:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "NOTIFICATION404_2", ref = "#/components/examples/NOTIFICATION404_2")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> readNotification(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "읽음 처리할 알림 ID", example = "24") Long notificationId
    );

    @Operation(tags = "알림", summary = "알림 목록 조회",
            description = "읽지 않은 알림을 먼저 보여주고, 같은 읽음 상태에서는 최신순으로 조회합니다. 밴드 멤버 초대 알림에만 bandInvite가 포함될 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "알림 목록을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = NotificationApiResponseSchemas.NotificationListPageEnvelope.class), examples = {
                            @ExampleObject(name = "알림 목록", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_4",
                                      "message": "알림 목록을 조회했습니다.",
                                      "result": {
                                        "items": [
                                          {
                                            "notificationId": 24,
                                            "type": "BAND_INVITE",
                                            "title": "OoF에서 멤버 초대를 보냈어요",
                                            "body": "밴드 멤버로 함께 활동해 주세요.",
                                            "deepLink": "/notifications?focusInviteId=7",
                                            "referenceId": 7,
                                            "isRead": false,
                                            "readAt": null,
                                            "createdAt": "2026-07-25T02:10:00",
                                            "bandInvite": {
                                              "bandMemberId": 7,
                                              "bandId": 1,
                                              "bandName": "OoF",
                                              "bandProfileImageUrl": null,
                                              "genre": "ETC",
                                              "region": "SEOUL",
                                              "acceptedMemberCount": 3,
                                              "memberType": "MEMBER",
                                              "status": "INVITED",
                                              "actionable": true
                                            }
                                          },
                                          {
                                            "notificationId": 23,
                                            "type": "SESSION",
                                            "title": "새로운 지원서가 도착했어요",
                                            "body": "테스트님이 드럼 세션 모집에 지원했어요.",
                                            "deepLink": "/sessions/recruitments/submissions/4",
                                            "referenceId": 4,
                                            "isRead": false,
                                            "readAt": null,
                                            "createdAt": "2026-07-25T02:00:00",
                                            "bandInvite": null
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursor": 23,
                                          "hasNext": true
                                        }
                                      },
                                      "timeStamp": "2026-07-25 02:12:22"
                                    }
                                    """),
                            @ExampleObject(name = "빈 목록", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_4",
                                      "message": "알림 목록을 조회했습니다.",
                                      "result": {
                                        "items": [],
                                        "pageInfo": {
                                          "nextCursor": null,
                                          "hasNext": false
                                        }
                                      },
                                      "timeStamp": "2026-07-25 02:12:22"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<CursorPage<NotificationListItemResponse>>> getNotifications(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "이전 응답의 pageInfo.nextCursor를 그대로 전달합니다. 첫 요청은 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본 20, 범위 1~50") @Min(1) @Max(50) int size
    );
}
