package com.umc.bscene.domain.notification.repository;

import com.umc.bscene.domain.notification.entity.Notification;
import com.umc.bscene.global.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 같은 쪽지방의 과거 MESSAGE를 제외하고 읽지 않은 알림 존재 여부를 조회
    @Query("""
        SELECT CASE
            WHEN COUNT(notification) > 0 THEN true
            ELSE false
        END
        FROM Notification notification
        WHERE notification.user.id = :userId
          AND notification.isRead = false
          AND (
              notification.type
                  <> com.umc.bscene.global.notification.enums.NotificationType.MESSAGE
              OR notification.referenceId IS NULL
              OR NOT EXISTS (
                  SELECT newerNotification.id
                  FROM Notification newerNotification
                  WHERE newerNotification.user.id = notification.user.id
                    AND newerNotification.type
                        = com.umc.bscene.global.notification.enums.NotificationType.MESSAGE
                    AND newerNotification.referenceId = notification.referenceId
                    AND newerNotification.id > notification.id
              )
          )
        """)
    boolean existsByUser_IdAndIsReadFalse(
            @Param("userId") Long userId
    );

    // 사용자가 소유한 알림을 조회
    Optional<Notification> findByIdAndUser_Id(Long notificationId, Long userId);

    // 같은 쪽지방에 저장된 기존 쪽지 알림을 삭제
    long deleteByUser_IdAndTypeAndReferenceId(
            Long userId,
            NotificationType type,
            Long referenceId
    );

    // 읽지 않은 알림을 먼저, 같은 읽음 상태에서는 최신순으로 조회
    @Query("""
        SELECT notification
        FROM Notification notification
        WHERE notification.user.id = :userId
          AND (
              notification.type
                  <> com.umc.bscene.global.notification.enums.NotificationType.MESSAGE
              OR notification.referenceId IS NULL
              OR NOT EXISTS (
                  SELECT newerNotification.id
                  FROM Notification newerNotification
                  WHERE newerNotification.user.id = notification.user.id
                    AND newerNotification.type
                        = com.umc.bscene.global.notification.enums.NotificationType.MESSAGE
                    AND newerNotification.referenceId = notification.referenceId
                    AND newerNotification.id > notification.id
              )
          )
          AND (
              :cursorId IS NULL
              OR (
                  :cursorIsRead = false
                  AND (
                      (
                          notification.isRead = false
                          AND notification.id < :cursorId
                      )
                      OR notification.isRead = true
                  )
              )
              OR (
                  :cursorIsRead = true
                  AND notification.isRead = true
                  AND notification.id < :cursorId
              )
          )
        ORDER BY
            CASE
                WHEN notification.isRead = false THEN 0
                ELSE 1
            END ASC,
            notification.id DESC
        """)
    List<Notification> findNotificationPage(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            @Param("cursorIsRead") boolean cursorIsRead,
            Pageable pageable
    );

    // 읽은 후 보관 기간이 지난 알림을 삭제
    long deleteByIsReadTrueAndReadAtBefore(LocalDateTime threshold);
}
