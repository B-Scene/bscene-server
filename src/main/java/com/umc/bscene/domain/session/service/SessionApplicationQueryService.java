package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSearchResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationSummaryResponse;
import com.umc.bscene.domain.session.dto.application.response.MyApplicationSubmissionListResponse;
import com.umc.bscene.domain.session.dto.application.response.SubmittedApplicationDetailResponse;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.enums.SkillLevel;

public interface SessionApplicationQueryService {

    MySessionApplicationDetailResponse getMySessionApplicationDetail(
            Long userId,
            Long sessionApplicationId
    );

    MySessionApplicationSummaryResponse getMySessionApplicationSummary(Long userId);

    MyApplicationSubmissionListResponse getMyApplicationSubmissions(
            Long userId,
            Long cursorId,
            Integer size
    );

    SubmittedApplicationDetailResponse getSubmittedApplication(
            Long viewerId,
            Long applicationSubmissionId
    );

    SessionApplicationDetailResponse getDefaultApplicationDetail(Long sessionApplicationId);

    SessionApplicationSearchResponse searchDefaultApplications(
            Long viewerUserId,
            Region region,
            SkillLevel skillLevel,
            Part part,
            Genre genre,
            String keyword,
            Long cursorId,
            Integer size
    );
}
