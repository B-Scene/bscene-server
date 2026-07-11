package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSearchResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationSummaryResponse;
import com.umc.bscene.domain.session.dto.application.response.MyApplicationSubmissionListResponse;
import com.umc.bscene.domain.session.dto.application.response.SubmittedApplicationDetailResponse;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;

import java.util.List;

public interface SessionApplicationQueryService {

    List<MySessionApplicationResponse> getMySessionApplications(Long userId);

    MySessionApplicationSummaryResponse getMySessionApplicationSummary(Long userId);

    MyApplicationSubmissionListResponse getMyApplicationSubmissions(
            Long userId,
            Long cursorId,
            Integer size
    );

    SubmittedApplicationDetailResponse getSubmittedApplication(
            Long ownerId,
            Long applicationSubmissionId
    );

    SessionApplicationDetailResponse getDefaultApplicationDetail(Long sessionApplicationId);

    SessionApplicationSearchResponse searchDefaultApplications(
            Long viewerUserId,
            SessionRegion region,
            SkillLevel skillLevel,
            Part part,
            String keyword,
            Long cursorId,
            Integer size
    );
}
