package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
        "checkedAt",
        "sessionRecruitmentId",
        "recruitmentTitle",
        "bandId",
        "bandName",
        "deadlineAt",
        "sessionApplicationId",
        "userId",
        "nickname",
        "profileImageUrl",
        "isPublic",
        "title",
        "purpose",
        "oneLineIntro",
        "part",
        "skillLevel",
        "genre",
        "region",
        "intro",
        "availableActivities",
        "careers",
        "portfolioLinks"
})
public record SubmittedApplicationDetailResponse(
        Long applicationSubmissionId,
        LocalDateTime checkedAt,
        Long sessionRecruitmentId,
        String recruitmentTitle,
        Long bandId,
        String bandName,
        LocalDateTime deadlineAt,
        Long sessionApplicationId,
        Long userId,
        String nickname,
        String profileImageUrl,
        Boolean isPublic,
        String title,
        String purpose,
        String oneLineIntro,
        Part part,
        SkillLevel skillLevel,
        @SessionGenreFormat Genre genre,
        @SessionRegionFormat Region region,
        String intro,
        List<AvailableActivity> availableActivities,
        List<MySessionApplicationResponse.CareerResponse> careers,
        List<SessionApplicationDetailResponse.PortfolioLinkResponse> portfolioLinks
) {
    public static SubmittedApplicationDetailResponse of(
            SessionApplicationSubmission submission,
            SessionApplicationDetailResponse application
    ) {
        SessionRecruitment recruitment = submission.getSessionRecruitment();
        return new SubmittedApplicationDetailResponse(
                submission.getApplicationSubmissionId(),
                submission.getCheckedAt(),
                recruitment.getSessionRecruitmentId(),
                recruitment.getRecruitmentTitle(),
                recruitment.getBand().getId(),
                recruitment.getBand().getName(),
                recruitment.getDeadlineAt(),
                application.getSessionApplicationId(),
                application.getUserId(),
                application.getNickname(),
                application.getProfileImageUrl(),
                application.getIsPublic(),
                application.getTitle(),
                application.getPurpose(),
                application.getOneLineIntro(),
                application.getPart(),
                application.getSkillLevel(),
                application.getGenre(),
                application.getRegion(),
                application.getIntro(),
                application.getAvailableActivities(),
                application.getCareers(),
                application.getPortfolioLinks()
        );
    }
}
