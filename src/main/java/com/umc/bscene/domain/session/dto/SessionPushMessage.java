package com.umc.bscene.domain.session.dto;


import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.notification.message.PushMessage;

public record SessionPushMessage(
        NotificationType type,
        String title,
        String body,
        String deepLink,
        Long referenceId
) implements PushMessage {

    // 세션 지원서 접수 알림
    public static SessionPushMessage applicationSubmitted(
        Long applicationSubmissionId,
        String applicationNickname,
        String recruitmentTitle
    ) {
        return new SessionPushMessage(
                NotificationType.SESSION,
                "새로운 지원서가 도착했어요",
                applicationNickname + "님이 '" + recruitmentTitle + "' 모집에 지원했어요.",
                "/sessions/recruitments/submissions/" + applicationSubmissionId,
                applicationSubmissionId
        );
    }
}
