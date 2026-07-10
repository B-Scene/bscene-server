package com.umc.bscene.domain.notification.adapter;

import com.umc.bscene.domain.notification.service.NotificationService;
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.global.notification.port.NotificationPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationAdapter implements NotificationPort {

    private final NotificationService notificationService;

    @Override
    public void send(Long receiverId, PushMessage message) {
        notificationService.send(receiverId, message);
    }
}