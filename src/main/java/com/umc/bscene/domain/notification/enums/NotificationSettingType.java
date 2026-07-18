package com.umc.bscene.domain.notification.enums;

import com.umc.bscene.domain.user.enums.UserMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationSettingType {

    // 팬 모드 공연 알림
    FAN_FOLLOWED_BAND_PERFORMANCE(UserMode.FAN, true),
    FAN_PERFORMANCE_REMINDER(UserMode.FAN, true),
    FAN_PERFORMANCE_UPDATE(UserMode.FAN, true),

    // 팬 모드 라이브 알림
    FAN_FOLLOWED_BAND_LIVE_START(UserMode.FAN, true),
    FAN_SCHEDULED_LIVE_REMINDER(UserMode.FAN, true),
    FAN_LIVE_REPLAY_READY(UserMode.FAN, true),

    // 밴드 모드 모집 알림
    BAND_NEW_SESSION_APPLICATION(UserMode.BAND, true),
    BAND_SESSION_APPLICATION_STATUS(UserMode.BAND, true),
    BAND_SESSION_RECRUITMENT_DEADLINE(UserMode.BAND, true),

    // 밴드 모드 라이브 알림
    BAND_SCHEDULED_LIVE_REMINDER(UserMode.BAND, true),
    BAND_LIVE_START_STATUS(UserMode.BAND, true);

    private final UserMode mode;
    private final boolean defaultEnabled;
}