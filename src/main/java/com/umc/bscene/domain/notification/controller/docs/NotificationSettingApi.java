package com.umc.bscene.domain.notification.controller.docs;

import com.umc.bscene.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingItemResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingsResponse;
import com.umc.bscene.global.notification.enums.NotificationSettingMode;
import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.NotificationApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

public interface NotificationSettingApi {

    @Operation(tags = "알림", summary = "알림 설정 조회",
            description = "팬 모드 또는 밴드 모드의 알림 설정을 조회합니다. 저장된 설정이 없으면 각 항목의 기본값을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "알림 설정을 조회했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = NotificationApiResponseSchemas.NotificationSettingsResponseEnvelope.class), examples = {
                            @ExampleObject(name = "팬 모드 기본 설정", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_6",
                                      "message": "알림 설정을 조회했습니다.",
                                      "result": {
                                        "mode": "FAN",
                                        "settings": [
                                          {
                                            "settingType": "FAN_FOLLOWED_BAND_PERFORMANCE",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "FAN_PERFORMANCE_REMINDER",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "FAN_PERFORMANCE_UPDATE",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "FAN_FOLLOWED_BAND_LIVE_START",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "FAN_SCHEDULED_LIVE_REMINDER",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "FAN_LIVE_REPLAY_READY",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "FAN_SESSION_APPLICATION_STATUS",
                                            "enabled": true
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-18 14:30:00"
                                    }
                                    """),
                            @ExampleObject(name = "밴드 모드 기본 설정", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_6",
                                      "message": "알림 설정을 조회했습니다.",
                                      "result": {
                                        "mode": "BAND",
                                        "settings": [
                                          {
                                            "settingType": "BAND_NEW_SESSION_APPLICATION",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "BAND_SESSION_APPLICATION_STATUS",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "BAND_SESSION_RECRUITMENT_DEADLINE",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "BAND_SCHEDULED_LIVE_REMINDER",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "BAND_LIVE_START_STATUS",
                                            "enabled": true
                                          },
                                          {
                                            "settingType": "BAND_LIVE_CO_HOST_INVITATION",
                                            "enabled": true
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-07-18 14:30:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<NotificationSettingsResponse>> getNotificationSettings(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "조회할 모드 (FAN 또는 BAND)", example = "FAN") NotificationSettingMode mode
    );

    @Operation(tags = "알림", summary = "알림 설정 변경",
            description = "선택한 알림 설정의 수신 여부를 변경합니다. enabled가 true면 수신하고 false면 수신을 해제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NotificationSettingUpdateRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "enabled": false
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "알림 설정을 변경했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = NotificationApiResponseSchemas.NotificationSettingItemResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "NOTIFICATION200_7",
                                      "message": "알림 설정을 변경했습니다.",
                                      "result": {
                                        "settingType": "FAN_LIVE_REPLAY_READY",
                                        "enabled": false
                                      },
                                      "timeStamp": "2026-07-18 14:31:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<NotificationSettingItemResponse>> updateNotificationSetting(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "변경할 알림 설정 종류", example = "FAN_LIVE_REPLAY_READY") NotificationSettingType settingType,
            NotificationSettingUpdateRequest request
    );
}
