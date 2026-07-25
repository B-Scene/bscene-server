package com.umc.bscene.global.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationSettingType {

    // 팬 모드 공연 알림
    FAN_FOLLOWED_BAND_PERFORMANCE(NotificationSettingMode.FAN, true),
    FAN_PERFORMANCE_REMINDER(NotificationSettingMode.FAN, true),
    FAN_PERFORMANCE_UPDATE(NotificationSettingMode.FAN, true),

    // 팬 모드 라이브 알림
    FAN_FOLLOWED_BAND_LIVE_START(NotificationSettingMode.FAN, true),
    FAN_SCHEDULED_LIVE_REMINDER(NotificationSettingMode.FAN, true),
    FAN_LIVE_REPLAY_READY(NotificationSettingMode.FAN, true),

    // 팬 모드 세션 지원 알림
    FAN_SESSION_APPLICATION_STATUS(NotificationSettingMode.FAN, true),

    // 밴드 모드 모집 알림
    BAND_NEW_SESSION_APPLICATION(NotificationSettingMode.BAND, true),
    BAND_SESSION_APPLICATION_STATUS(NotificationSettingMode.BAND, true),
    BAND_SESSION_RECRUITMENT_DEADLINE(NotificationSettingMode.BAND, true),

    // 밴드 모드 라이브 알림
    BAND_SCHEDULED_LIVE_REMINDER(NotificationSettingMode.BAND, true),
    BAND_LIVE_START_STATUS(NotificationSettingMode.BAND, true);

    private final NotificationSettingMode mode;
    private final boolean defaultEnabled;
}