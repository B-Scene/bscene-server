package com.umc.bscene.domain.session.dto.recruitment.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.entity.SessionRecruitmentInterest;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.converter.SessionGenreFormat;
import com.umc.bscene.domain.session.converter.SessionRegionFormat;
import com.umc.bscene.domain.session.enums.SkillLevel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@JsonPropertyOrder({
        "interestId", "sessionRecruitmentId", "dDay", "isClosed", "isInterested",
        "recruitmentTitle", "bandName", "bandGenre", "bandRegion",
        "postedAgo", "summary", "part", "skillLevel"
})
public record InterestedRecruitmentItemResponse(
        Long interestId,
        Long sessionRecruitmentId,
        long dDay,
        boolean isClosed,
        boolean isInterested,
        String recruitmentTitle,
        String bandName,
        @SessionGenreFormat Genre bandGenre,
        @SessionRegionFormat Region bandRegion,
        long postedAgo,
        String summary,
        Part part,
        SkillLevel skillLevel
) {
    public static InterestedRecruitmentItemResponse from(
            SessionRecruitmentInterest interest
    ) {
        SessionRecruitment recruitment = interest.getSessionRecruitment();
        return new InterestedRecruitmentItemResponse(
                interest.getSessionRecruitmentInterestId(),
                recruitment.getSessionRecruitmentId(),
                ChronoUnit.DAYS.between(
                        LocalDateTime.now().toLocalDate(),
                        recruitment.getDeadlineAt().toLocalDate()
                ),
                !recruitment.getDeadlineAt().isAfter(LocalDateTime.now()),
                true,
                recruitment.getRecruitmentTitle(),
                recruitment.getBand().getName(),
                recruitment.getBand().getGenre(),
                recruitment.getBand().getRegion(),
                Math.max(0, ChronoUnit.DAYS.between(
                        recruitment.getCreatedAt().toLocalDate(),
                        LocalDateTime.now().toLocalDate()
                )),
                recruitment.getSummary(),
                recruitment.getPart(),
                recruitment.getSkillLevel()
        );
    }
}
