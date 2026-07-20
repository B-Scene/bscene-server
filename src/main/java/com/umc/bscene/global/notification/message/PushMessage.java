package com.umc.bscene.global.notification.message;

import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.notification.enums.NotificationType;

public interface PushMessage {

    NotificationType type();

    // null이면 사용자 알림 설정과 관계없이 항상 발송
    default NotificationSettingType settingType() {
        return null;
    }

    String title();
    String body();
    String deepLink();
    Long referenceId();
}