package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.request.SessionApplicationVisibilityRequest;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationVisibilityResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSubmitResponse;

public interface SessionApplicationCommandService {

    MySessionApplicationResponse createSessionApplication(
            Long userId,
            MySessionApplicationUpdateRequest request
    );

    MySessionApplicationResponse updateSessionApplication(
            Long userId,
            Long sessionApplicationId,
            MySessionApplicationUpdateRequest request
    );

    void deleteSessionApplication(Long userId, Long sessionApplicationId);

    SessionApplicationVisibilityResponse updateVisibility(
            Long userId,
            Long sessionApplicationId,
            SessionApplicationVisibilityRequest request
    );

    SessionApplicationSubmitResponse submitApplication(
            Long userId,
            Long sessionRecruitmentId,
            Long sessionApplicationId
    );

    void cancelSubmission(Long userId, Long applicationSubmissionId);
}
