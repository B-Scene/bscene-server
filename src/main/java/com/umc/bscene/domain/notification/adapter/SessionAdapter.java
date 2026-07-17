package com.umc.bscene.domain.notification.adapter;

import com.umc.bscene.domain.notification.exception.NotificationException;
import com.umc.bscene.domain.session.port.NotifyPort;
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.global.notification.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SessionAdapter implements NotifyPort {

    private final NotificationPort notificationPort;

    @Override
    public void notify(List<Long> receiverIds, PushMessage message) {
        for (Long receiverId : receiverIds) {
            try {
                notificationPort.send(receiverId, message);
            } catch (NotificationException exception) {
                log.warn(
                        "세션 지원 알림 발송 실패: receiverId={}, code={}",
                        receiverId,
                        exception.getBaseResponseCode().getCode(),
                        exception
                );
            } catch (RuntimeException exception) {
                log.error(
                        "세션 지원 알림 처리 중 예상하지 못한 오류: receiverId={}",
                        receiverId,
                        exception
                );
            }
        }
    }
}