package com.umc.bscene.domain.user.dto.response.session;

import com.umc.bscene.domain.session.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SessionRecruitmentResponse(
        Long recruitmentPostId,
        LocalDateTime dueDate,
        String title,
        String part,
        String genre,
        String region,
        List<Recruiter> recruiters
) {
    public record Recruiter(
            Long sessionProfileId,
            Long applySubmissionId,
            String profileImageUrl,
            String nickname,
            String part,
            String level,
            String region,
            ApplicationStatus status
    ) {}
}
