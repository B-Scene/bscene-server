package com.umc.bscene.domain.notification.repository;

import com.umc.bscene.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // FanHomeAdapter에서 사용 : 사용자의 읽지 않은 알림 존재 여부 (exists → 첫 행만 확인)
    boolean existsByUser_IdAndIsReadFalse(Long userId);

    // 사용자의 알림을 notificationId 기준 최신순으로 조회
    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.user.id = :userId
              AND (:cursor IS NULL OR notification.id < :cursor)
            ORDER BY notification.id DESC
            """)
    List<Notification> findNotificationPage(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
