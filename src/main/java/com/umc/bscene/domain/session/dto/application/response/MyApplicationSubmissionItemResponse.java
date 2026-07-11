package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@JsonPropertyOrder({
        "applicationSubmissionId",
        "sessionRecruitmentId",
        "sessionApplicationId",
        "status",
        "recruitmentTitle",
        "bandName",
        "appliedAgo",
        "checkedAt"
})
public record MyApplicationSubmissionItemResponse(
        Long applicationSubmissionId,
        Long sessionRecruitmentId,
        Long sessionApplicationId,
        String status,
        String recruitmentTitle,
        String bandName,
        long appliedAgo,
        LocalDateTime checkedAt
) {
    public static MyApplicationSubmissionItemResponse from(
            SessionApplicationSubmission submission
    ) {
        return new MyApplicationSubmissionItemResponse(
                submission.getApplicationSubmissionId(),
                submission.getSessionRecruitment().getSessionRecruitmentId(),
                submission.getSessionApplication().getSessionApplicationId(),
                statusLabel(submission),
                submission.getSessionRecruitment().getRecruitmentTitle(),
                submission.getSessionRecruitment().getBand().getName(),
                appliedAgo(submission.getCreatedAt()),
                submission.getCheckedAt()
        );
    }

    private static String statusLabel(SessionApplicationSubmission submission) {
        return switch (submission.getStatus()) {
            case PENDING -> "지원 완료";
            case ACCEPTED -> "지원 수락";
            case REJECTED -> "지원 거절";
            case CANCELED -> "지원 취소";
        };
    }

    private static long appliedAgo(LocalDateTime appliedAt) {
        return Math.max(0, ChronoUnit.DAYS.between(
                appliedAt.toLocalDate(),
                LocalDateTime.now().toLocalDate()
        ));
    }
}
