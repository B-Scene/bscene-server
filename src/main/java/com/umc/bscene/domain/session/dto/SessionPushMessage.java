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

    // 밴드 측 지원 수락·거절 결과를 지원자에게 알림
    public static SessionPushMessage applicationDecisionForApplicant(
            Long applicationSubmissionId,
            String recruitmentTitle,
            boolean isApproved
    ) {
        return new SessionPushMessage(
                NotificationType.SESSION,
                NotificationSettingType.FAN_SESSION_APPLICATION_STATUS,
                isApproved ? "세션 지원이 수락되었어요" : "세션 지원이 거절되었어요",
                isApproved
                        ? "'" + recruitmentTitle + "' 모집에서 지원을 수락했어요. 최종 참여 여부를 선택해주세요."
                        : "'" + recruitmentTitle + "' 모집에서 지원을 거절했어요.",
                "/sessions/applications/submissions?focusSubmissionId=" + applicationSubmissionId,
                applicationSubmissionId
        );
    }

    // 밴드 측 지원 처리 결과를 다른 밴드 구성원에게 알림
    public static SessionPushMessage applicationDecisionForBandMembers(
            Long applicationSubmissionId,
            String applicationNickname,
            boolean isApproved
    ) {
        return new SessionPushMessage(
                NotificationType.SESSION,
                NotificationSettingType.BAND_SESSION_APPLICATION_STATUS,
                "세션 지원서 상태가 변경되었어요",
                applicationNickname + "님의 지원을 " + (isApproved ? "수락했어요." : "거절했어요."),
                "/sessions/recruitments/submissions/" + applicationSubmissionId,
                applicationSubmissionId
        );
    }

    // 지원자의 최종 수락·거절 결과를 밴드 구성원에게 알림
    public static SessionPushMessage applicationFinalDecisionForBandMembers(
            Long applicationSubmissionId,
            String applicationNickname,
            boolean isAccepted
    ) {
        return new SessionPushMessage(
                NotificationType.SESSION,
                NotificationSettingType.BAND_SESSION_APPLICATION_STATUS,
                "지원자가 참여 여부를 결정했어요",
                applicationNickname + "님이 세션 참여를 " + (isAccepted ? "확정했어요." : "거절했어요."),
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
