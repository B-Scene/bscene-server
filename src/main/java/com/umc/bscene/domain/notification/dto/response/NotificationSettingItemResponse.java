package com.umc.bscene.domain.notification.dto.response;

import com.umc.bscene.global.notification.enums.NotificationSettingType;

public record NotificationSettingItemResponse(
        NotificationSettingType settingType,
        boolean enabled
) {
}