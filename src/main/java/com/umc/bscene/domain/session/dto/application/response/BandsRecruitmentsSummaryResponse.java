package com.umc.bscene.domain.session.dto.application.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;

import java.time.LocalDateTime;

public record BandsRecruitmentsSummaryResponse(
        Long recruitmentId,
        Long applySubmissionId,
        LocalDateTime deadline,
        String recruitPostTitle,
        String recruitPart,
        String recruitGenre,
        String recruitRegion,
        String applierProfileImageUrl,
        String applierName,
        String applierPart,
        String applierSkill,
        String applierRegion,
        ApplicationStatus status,
        boolean isChecked
) {
    public BandsRecruitmentsSummaryResponse(Long recruitmentId, Long applySubmissionId, LocalDateTime deadline, String recruitPostTitle, Part recruitPart, Genre recruitGenre, Region recruitRegion, String applierProfileImageUrl, String applierName, Part applierPart, SkillLevel applierSkill, Region applierRegion, ApplicationStatus status, LocalDateTime checkedAt) {
        this(recruitmentId, applySubmissionId, deadline, recruitPostTitle, recruitPart.getDescription(), recruitGenre.getName(), recruitRegion.getName(), applierProfileImageUrl, applierName, applierPart.getDescription(), applierSkill.getDescription(), applierRegion.getName(), status, checkedAt != null);
    }
}
