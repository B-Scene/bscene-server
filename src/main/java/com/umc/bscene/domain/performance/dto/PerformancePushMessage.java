package com.umc.bscene.domain.performance.dto;

import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.notification.message.PushMessage;

public record PerformancePushMessage(
        NotificationType type,
        NotificationSettingType settingType,
        String title,
        String body,
        String deepLink,
        Long referenceId
) implements PushMessage {

    // 팔로우한 밴드의 공연 등록 알림
    public static PerformancePushMessage created(
            String bandName,
            String performanceTitle,
            Long performanceId
    ) {
        return new PerformancePushMessage(
                NotificationType.PERFORMANCE,
                NotificationSettingType.FAN_FOLLOWED_BAND_PERFORMANCE,
                bandName + "의 새로운 공연이 등록됐어요",
                "'" + performanceTitle + "' 공연 정보를 확인해보세요.",
                "/fan/home/concerts/" + performanceId,
                performanceId
        );
    }

    // 공연 시작 1시간 전 알림
    public static PerformancePushMessage reminder(
            String bandName,
            String performanceTitle,
            Long performanceId
    ) {
        return new PerformancePushMessage(
                NotificationType.PERFORMANCE,
                NotificationSettingType.FAN_PERFORMANCE_REMINDER,
                "공연 시작 1시간 전이에요",
                bandName + "의 '" + performanceTitle + "' 공연이 곧 시작돼요.",
                "/fan/home/concerts/" + performanceId,
                performanceId
        );
    }

    // 공연 정보 변경 알림
    public static PerformancePushMessage updated(
            String bandName,
            String performanceTitle,
            Long performanceId
    ) {
        return new PerformancePushMessage(
                NotificationType.PERFORMANCE,
                NotificationSettingType.FAN_PERFORMANCE_UPDATE,
                "공연 정보가 변경됐어요",
                bandName + "의 '" + performanceTitle + "' 공연 정보를 다시 확인해주세요.",
                "/fan/home/concerts/" + performanceId,
                performanceId
        );
    }
}
