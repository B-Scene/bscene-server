package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;

public interface SessionRecruitmentQueryService {

    SessionRecruitmentListResponse getSessionRecruitments(
            Part part,
            SessionGenre genre,
            SessionRegion region,
            String keyword,
            Long cursorId,
            Integer size
    );
    SessionRecruitmentDetailResponse getSessionRecruitmentDetail(Long recruitmentId);
}