package com.umc.bscene.domain.notification.adapter;

import com.umc.bscene.domain.fanhome.port.NotificationPort;
import com.umc.bscene.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;

/**
 * 팬홈의 NotificationPort를 notification 도메인이 구현하는 어댑터.
 * 사용자의 읽지 않은 알림 존재 여부를 조회한다.
 */
@RequiredArgsConstructor
public class FanHomeAdapter implements NotificationPort {

    private final NotificationRepository notificationRepository;

    @Override
    public boolean hasUnread(Long userId) {
        return notificationRepository.existsUnreadByUserId(userId);
    }
}
