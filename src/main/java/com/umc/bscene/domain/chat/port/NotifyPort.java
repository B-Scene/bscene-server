package com.umc.bscene.domain.chat.port;

import com.umc.bscene.global.notification.message.PushMessage;

public interface NotifyPort {

    // 쪽지 수신자에게 푸시 알림 발송을 요청
    void notify(Long receiverId, PushMessage message);
}