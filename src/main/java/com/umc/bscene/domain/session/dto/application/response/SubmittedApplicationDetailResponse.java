package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.converter.SessionGenreFormat;
import com.umc.bscene.domain.session.converter.SessionRegionFormat;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.AvailableActivity;

import java.time.LocalDateTime;
import java.util.List;

@JsonPropertyOrder({
        "applicationSubmissionId",
        "sessionRecruitmentId",
        "recruitmentTitle",
        "bandId",
        "bandName",
        "deadlineAt",
        "sessionApplicationId",
        "title",
        "userId",
        "profileImageUrl",
        "nickname",
        "defaultPart",
        "defaultSkillLevel",
        "defaultRegion",
        "isPublic",
        "purpose",
        "oneLineIntro",
        "intro",
        "part",
        "skillLevel",
        "genre",
        "region",
        "availableActivities",
        "careers",
        "portfolioLinks"
})
public record SubmittedApplicationDetailResponse(
        Long applicationSubmissionId,
        Long sessionRecruitmentId,
        String recruitmentTitle,
        Long bandId,
        String bandName,
        LocalDateTime deadlineAt,
        Long sessionApplicationId,
        String title,
        Long userId,
        String profileImageUrl,
        String nickname,
        Part defaultPart,
        SkillLevel defaultSkillLevel,
        @SessionRegionFormat Region defaultRegion,
        Boolean isPublic,
        String purpose,
        String oneLineIntro,
        String intro,
        Part part,
        SkillLevel skillLevel,
        @SessionGenreFormat Genre genre,
        @SessionRegionFormat Region region,
        List<AvailableActivity> availableActivities,
        List<MySessionApplicationResponse.CareerResponse> careers,
        List<SessionApplicationDetailResponse.PortfolioLinkResponse> portfolioLinks
) {
    public static SubmittedApplicationDetailResponse of(
            SessionApplicationSubmission submission,
            SessionApplicationDetailResponse application,
            SessionApplication defaultApplication
    ) {
        SessionRecruitment recruitment = submission.getSessionRecruitment();
        return new SubmittedApplicationDetailResponse(
                submission.getApplicationSubmissionId(),
                recruitment.getSessionRecruitmentId(),
                recruitment.getRecruitmentTitle(),
                recruitment.getBand().getId(),
                recruitment.getBand().getName(),
                recruitment.getDeadlineAt(),
                application.getSessionApplicationId(),
                application.getTitle(),
                application.getUserId(),
                application.getProfileImageUrl(),
                application.getNickname(),
                defaultApplication == null ? null : defaultApplication.getPart(),
                defaultApplication == null ? null : defaultApplication.getSkillLevel(),
                defaultApplication == null ? null : defaultApplication.getRegion(),
                application.getIsPublic(),
                application.getPurpose(),
                application.getOneLineIntro(),
                application.getIntro(),
                application.getPart(),
                application.getSkillLevel(),
                application.getGenre(),
                application.getRegion(),
                application.getAvailableActivities(),
                application.getCareers(),
                application.getPortfolioLinks()
        );
    }
}
