package com.umc.bscene.global.notification.port;

import com.umc.bscene.global.notification.message.PushMessage;

public interface NotificationPort {

    // 다른 도메인에서 알림 발송을 요청할 때 사용하는 공통 진입점
    void send(Long receiverId, PushMessage message);
}