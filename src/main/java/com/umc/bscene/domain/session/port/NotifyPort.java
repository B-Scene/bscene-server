package com.umc.bscene.domain.session.port;

import com.umc.bscene.global.notification.message.PushMessage;

public interface NotifyPort {

    // 세션 도메인에서 사용자에게 알림 발송을 요청
    void notify(Long receiverId, PushMessage message);
}
