package com.umc.bscene.domain.notification.repository;

import com.umc.bscene.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // FanHomeAdapter에서 사용 : 사용자의 읽지 않은 알림 존재 여부 (exists → 첫 행만 확인)
    boolean existsByUser_IdAndIsReadFalse(Long userId);
}
