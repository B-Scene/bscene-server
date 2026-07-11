package com.umc.bscene.global.notification.message;

import com.umc.bscene.global.notification.enums.NotificationType;

public interface PushMessage {

    NotificationType type();
    String title();
    String body();
    String deepLink();
    Long referenceId();
}