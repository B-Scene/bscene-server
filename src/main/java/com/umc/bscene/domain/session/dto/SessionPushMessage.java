package com.umc.bscene.domain.session.dto;


import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.notification.message.PushMessage;

public record SessionPushMessage(
        NotificationType type,
        NotificationSettingType settingType,
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
                NotificationSettingType.BAND_NEW_SESSION_APPLICATION,
                "새로운 지원서가 도착했어요",
                applicationNickname + "님이 '" + recruitmentTitle + "' 모집에 지원했어요.",
                "/sessions/recruitments/submissions/" + applicationSubmissionId,
                applicationSubmissionId
        );
    }

    // 세션 모집 마감 24시간 전 알림
    public static SessionPushMessage deadlineReminder(
            String recruitmentTitle,
            Long sessionRecruitmentId
    ) {
        return new SessionPushMessage(
                NotificationType.SESSION,
                NotificationSettingType.BAND_SESSION_RECRUITMENT_DEADLINE,
                "세션 모집 마감이 하루 남았어요",
                "'" + recruitmentTitle + "' 모집 공고를 확인해주세요.",
                "/sessions/recruitments/" + sessionRecruitmentId,
                sessionRecruitmentId
        );
    }
}
