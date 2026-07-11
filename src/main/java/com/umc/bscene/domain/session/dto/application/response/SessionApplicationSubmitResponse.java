package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;

@JsonPropertyOrder({
        "applicationSubmissionId",
        "recruitmentTitle",
        "bandName",
        "applicationTitle"
})
public record SessionApplicationSubmitResponse(
        Long applicationSubmissionId,
        String recruitmentTitle,
        String bandName,
        String applicationTitle
) {
    public static SessionApplicationSubmitResponse from(SessionApplicationSubmission submission) {
        return new SessionApplicationSubmitResponse(
                submission.getApplicationSubmissionId(),
                submission.getSessionRecruitment().getRecruitmentTitle(),
                submission.getSessionRecruitment().getBand().getName(),
                submission.getSessionApplication().getTitle()
        );
    }
}
