package com.umc.bscene.domain.user.dto.response.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umc.bscene.domain.session.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SessionRecruitmentResponse(
        Long recruitmentPostId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime dueDate,
        String title,
        String part,
        String genre,
        String region,
        int totalApplicants,
        List<Recruiter> recruiters
) {
    public record Recruiter(
            Long applySubmissionId,
            String profileImageUrl,
            String name,
            String part,
            String level,
            String region,
            ApplicationStatus status,
            boolean isChecked
    ) {}
}
