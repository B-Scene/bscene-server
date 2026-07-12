package com.umc.bscene.domain.session.dto.recruitment.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.entity.SessionRecruitmentInterest;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@JsonPropertyOrder({
        "interestId", "sessionRecruitmentId", "isClosed", "recruitmentTitle",
        "part", "skillLevel", "practiceSchedule", "bandName", "region",
        "postedAgo", "applicationTitle"
})
public record InterestedRecruitmentItemResponse(
        Long interestId,
        Long sessionRecruitmentId,
        boolean isClosed,
        String recruitmentTitle,
        Part part,
        SkillLevel skillLevel,
        String practiceSchedule,
        String bandName,
        SessionRegion region,
        long postedAgo,
        String applicationTitle
) {
    public static InterestedRecruitmentItemResponse of(
            SessionRecruitmentInterest interest,
            String applicationTitle
    ) {
        SessionRecruitment recruitment = interest.getSessionRecruitment();
        return new InterestedRecruitmentItemResponse(
                interest.getSessionRecruitmentInterestId(),
                recruitment.getSessionRecruitmentId(),
                !recruitment.getDeadlineAt().isAfter(LocalDateTime.now()),
                recruitment.getRecruitmentTitle(),
                recruitment.getPart(),
                recruitment.getSkillLevel(),
                recruitment.getPracticeSchedule(),
                recruitment.getBand().getName(),
                recruitment.getRegion(),
                Math.max(0, ChronoUnit.DAYS.between(
                        recruitment.getCreatedAt().toLocalDate(),
                        LocalDateTime.now().toLocalDate()
                )),
                applicationTitle
        );
    }
}
