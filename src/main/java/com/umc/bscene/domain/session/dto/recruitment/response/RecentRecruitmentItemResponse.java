package com.umc.bscene.domain.session.dto.recruitment.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.entity.SessionRecruitmentView;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record RecentRecruitmentItemResponse(
        Long viewId,
        Long sessionRecruitmentId,
        boolean isClosed,
        String recruitmentTitle,
        Part part,
        SkillLevel skillLevel,
        String practiceSchedule,
        String bandName,
        SessionRegion region,
        long viewedAgo,
        @JsonInclude(JsonInclude.Include.NON_NULL) String applicationTitle
) {
    public static RecentRecruitmentItemResponse of(
            SessionRecruitmentView view,
            String applicationTitle
    ) {
        SessionRecruitment recruitment = view.getSessionRecruitment();
        return new RecentRecruitmentItemResponse(
                view.getSessionRecruitmentViewId(),
                recruitment.getSessionRecruitmentId(),
                !recruitment.getDeadlineAt().isAfter(LocalDateTime.now()),
                recruitment.getRecruitmentTitle(),
                recruitment.getPart(),
                recruitment.getSkillLevel(),
                recruitment.getPracticeSchedule(),
                recruitment.getBand().getName(),
                recruitment.getRegion(),
                Math.max(0, ChronoUnit.DAYS.between(
                        view.getCreatedAt().toLocalDate(),
                        LocalDateTime.now().toLocalDate()
                )),
                applicationTitle
        );
    }
}
