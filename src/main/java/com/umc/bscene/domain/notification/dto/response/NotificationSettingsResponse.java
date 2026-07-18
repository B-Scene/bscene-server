package com.umc.bscene.domain.notification.dto.response;

import com.umc.bscene.domain.user.enums.UserMode;

import java.util.List;

public record NotificationSettingsResponse(
        UserMode mode,
        List<NotificationSettingItemResponse> settings
) {
}