package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;

import java.time.LocalDateTime;

@JsonPropertyOrder({"applicationSubmissionId", "checkedAt", "application"})
public record SubmittedApplicationDetailResponse(
        Long applicationSubmissionId,
        LocalDateTime checkedAt,
        SessionApplicationDetailResponse application
) {
    public static SubmittedApplicationDetailResponse of(
            SessionApplicationSubmission submission,
            SessionApplicationDetailResponse application
    ) {
        return new SubmittedApplicationDetailResponse(
                submission.getApplicationSubmissionId(),
                submission.getCheckedAt(),
                application
        );
    }
}
