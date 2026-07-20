package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.RecentRecruitmentListResponse;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.SessionRecruitmentSortType;

public interface SessionRecruitmentQueryService {

    SessionRecruitmentListResponse getSessionRecruitments(
            Long userId,
            Part part,
            SkillLevel skillLevel,
            Genre genre,
            Region region,
            String keyword,
            SessionRecruitmentSortType sort,
            Long cursorId,
            Integer size
    );
    SessionRecruitmentDetailResponse getSessionRecruitmentDetail(Long userId, Long recruitmentId);

    RecentRecruitmentListResponse getRecentRecruitments(
            Long userId,
            Long cursorId,
            Integer size
    );
}
