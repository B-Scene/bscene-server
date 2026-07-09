package com.umc.bscene.domain.notification.service;

import com.umc.bscene.domain.notification.port.PushSender;

public class NoopPushSender implements PushSender {

    @Override
    public void send(String targetToken, String title, String body) {
        // FCM 비활성화 상태에서는 실제 푸시를 발송 X
    }
}