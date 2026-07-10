package com.umc.bscene.domain.notification.adapter;

import com.umc.bscene.domain.notification.port.PushPort;

public class NoopPushAdapter implements PushPort {

    @Override
    public void send(String targetToken, String title, String body) {
        // FCM 비활성화 상태에서는 실제 푸시를 발송 X
    }
}