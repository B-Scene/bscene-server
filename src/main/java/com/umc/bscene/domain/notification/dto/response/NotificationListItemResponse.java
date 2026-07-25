package com.umc.bscene.domain.notification.dto.response;

import com.umc.bscene.domain.notification.entity.Notification;
import com.umc.bscene.global.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationListItemResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String body,
        String deepLink,
        Long referenceId,
        Boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        BandInviteNotificationDetailResponse bandInvite
) {

    public static NotificationListItemResponse from(Notification notification) {
        return from(notification, null);
    }

    public static NotificationListItemResponse from(
            Notification notification,
            BandInviteNotificationDetailResponse bandInvite
    ) {
        return new NotificationListItemResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getDeepLink(),
                notification.getReferenceId(),
                notification.getIsRead(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                bandInvite
        );
    }
}