package com.umc.bscene.domain.performance.port;

import com.umc.bscene.global.notification.message.PushMessage;

import java.util.List;

public interface NotifyPort {

    // 여러 사용자에게 공연 관련 푸시 알림 발송을 요청
    void notify(List<Long> receiverIds, PushMessage message);
}