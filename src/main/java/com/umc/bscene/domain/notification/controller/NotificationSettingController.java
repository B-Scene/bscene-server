package com.umc.bscene.domain.notification.controller;

import com.umc.bscene.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingItemResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingsResponse;
import com.umc.bscene.domain.notification.enums.NotificationSettingType;
import com.umc.bscene.domain.notification.response.code.NotificationSuccessCode;
import com.umc.bscene.domain.notification.service.NotificationService;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/notification-settings")
public class NotificationSettingController {

    private final NotificationService notificationService;

    // 사용자의 모드별 알림 설정 조회
    @GetMapping
    public ResponseEntity<SuccessResponse<NotificationSettingsResponse>> getNotificationSettings(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam UserMode mode
    ) {
        NotificationSettingsResponse response =
                notificationService.getNotificationSettings(
                        authMember.getUser().getId(),
                        mode
                );

        SuccessResponse<NotificationSettingsResponse> successResponse =
                SuccessResponse.of(
                        response,
                        NotificationSuccessCode.NOTIFICATION_SETTING_GET_SUCCESS
                );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 사용자의 특정 알림 설정 변경
    @PatchMapping("/{settingType}")
    public ResponseEntity<SuccessResponse<NotificationSettingItemResponse>> updateNotificationSetting(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable NotificationSettingType settingType,
            @Valid @RequestBody NotificationSettingUpdateRequest request
    ) {
        NotificationSettingItemResponse response =
                notificationService.updateNotificationSetting(
                        authMember.getUser().getId(),
                        settingType,
                        request
                );

        SuccessResponse<NotificationSettingItemResponse> successResponse =
                SuccessResponse.of(
                        response,
                        NotificationSuccessCode.NOTIFICATION_SETTING_UPDATE_SUCCESS
                );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}