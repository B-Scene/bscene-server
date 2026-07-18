package com.umc.bscene.domain.notification.dto.response;

import com.umc.bscene.domain.notification.enums.NotificationSettingType;

public record NotificationSettingItemResponse(
        NotificationSettingType settingType,
        boolean enabled
) {
}