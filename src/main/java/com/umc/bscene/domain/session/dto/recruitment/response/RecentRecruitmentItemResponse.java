package com.umc.bscene.domain.session.dto.recruitment.response;

import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.entity.SessionRecruitmentView;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.session.converter.SessionGenreFormat;
import com.umc.bscene.domain.session.converter.SessionRegionFormat;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.auth.enums.onboarding.Region;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record RecentRecruitmentItemResponse(
        Long viewId,
        Long sessionRecruitmentId,
        long dDay,
        boolean isClosed,
        String recruitmentTitle,
        String bandName,
        @SessionRegionFormat Region bandRegion,
        long viewedAgo,
        Part part,
        SkillLevel skillLevel,
        @SessionGenreFormat Genre genre
) {
    public static RecentRecruitmentItemResponse from(SessionRecruitmentView view) {
        SessionRecruitment recruitment = view.getSessionRecruitment();
        return new RecentRecruitmentItemResponse(
                view.getSessionRecruitmentViewId(),
                recruitment.getSessionRecruitmentId(),
                ChronoUnit.DAYS.between(
                        LocalDateTime.now().toLocalDate(),
                        recruitment.getDeadlineAt().toLocalDate()
                ),
                !recruitment.getDeadlineAt().isAfter(LocalDateTime.now()),
                recruitment.getRecruitmentTitle(),
                recruitment.getBand().getName(),
                recruitment.getBand().getRegion(),
                Math.max(0, ChronoUnit.DAYS.between(
                        view.getCreatedAt().toLocalDate(),
                        LocalDateTime.now().toLocalDate()
                )),
                recruitment.getPart(),
                recruitment.getSkillLevel(),
                recruitment.getGenre()
        );
    }
}
