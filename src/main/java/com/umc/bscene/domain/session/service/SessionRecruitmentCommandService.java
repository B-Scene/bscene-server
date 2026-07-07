package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentCreateRequest;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentUpdateRequest;
public interface SessionRecruitmentCommandService {

    SessionRecruitmentCreateResponse createSessionRecruitment(
            Long userId,
            SessionRecruitmentCreateRequest request
    );
    SessionRecruitmentCreateResponse updateSessionRecruitment(
            Long userId,
            Long sessionRecruitmentId,
            SessionRecruitmentUpdateRequest request
    );
    void deleteSessionRecruitment(
            Long userId,
            Long sessionRecruitmentId
    );

}