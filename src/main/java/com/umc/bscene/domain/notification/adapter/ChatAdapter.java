package com.umc.bscene.domain.notification.adapter;

import com.umc.bscene.domain.chat.port.NotifyPort;
import com.umc.bscene.domain.notification.exception.NotificationException;
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.global.notification.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ChatAdapter implements NotifyPort {

    private final NotificationPort notificationPort;

    @Override
    public void notify(Long receiverId, PushMessage message) {
        try {
            notificationPort.send(receiverId, message);
        } catch (NotificationException exception) {
            log.warn(
                    "쪽지 수신 알림 발송 실패: receiverId={}, code={}",
                    receiverId,
                    exception.getBaseResponseCode().getCode(),
                    exception
            );
        } catch (RuntimeException exception) {
            log.error(
                    "쪽지 수신 알림 처리 중 예상하지 못한 오류: receiverId={}",
                    receiverId,
                    exception
            );
        }
    }
}