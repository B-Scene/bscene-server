package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentCreateRequest;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;

public interface SessionRecruitmentCommandService {

    SessionRecruitmentCreateResponse createSessionRecruitment(
            Long userId,
            SessionRecruitmentCreateRequest request
    );
}