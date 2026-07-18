package com.umc.bscene.domain.notification.dto.response;

import com.umc.bscene.global.notification.enums.NotificationSettingMode;

import java.util.List;

public record NotificationSettingsResponse(
        NotificationSettingMode mode,
        List<NotificationSettingItemResponse> settings
) {
}