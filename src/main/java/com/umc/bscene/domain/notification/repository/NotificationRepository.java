package com.umc.bscene.domain.notification.repository;

import com.umc.bscene.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // FanHomeAdapter에서 사용 : 사용자의 읽지 않은 알림 존재 여부
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    boolean existsUnreadByUserId(@Param("userId") Long userId);
}
