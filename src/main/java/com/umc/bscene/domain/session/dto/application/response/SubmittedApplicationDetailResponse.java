package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;

import java.time.LocalDateTime;
import java.util.List;

@JsonPropertyOrder({
        "applicationSubmissionId",
        "checkedAt",
        "userId",
        "nickname",
        "profileImageUrl",
        "sessionRecruitmentId",
        "recruitmentTitle",
        "bandId",
        "bandName",
        "deadlineAt",
        "intro",
        "part",
        "skillLevel",
        "genre",
        "region",
        "portfolioLinks"
})
public record SubmittedApplicationDetailResponse(
        Long applicationSubmissionId,
        LocalDateTime checkedAt,
        Long userId,
        String nickname,
        String profileImageUrl,
        Long sessionRecruitmentId,
        String recruitmentTitle,
        Long bandId,
        String bandName,
        LocalDateTime deadlineAt,
        String intro,
        Part part,
        SkillLevel skillLevel,
        SessionGenre genre,
        SessionRegion region,
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
                application.getUserId(),
                application.getNickname(),
                application.getProfileImageUrl(),
                recruitment.getSessionRecruitmentId(),
                recruitment.getRecruitmentTitle(),
                recruitment.getBand().getId(),
                recruitment.getBand().getName(),
                recruitment.getDeadlineAt(),
                application.getIntro(),
                application.getPart(),
                application.getSkillLevel(),
                application.getGenre(),
                application.getRegion(),
                application.getPortfolioLinks()
        );
    }
}
