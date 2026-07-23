package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.global.response.CursorPage;

public interface SessionPort {

    CursorPage<SessionRecruitmentResponse> findRecruitmentsByBandId(
            Long bandId,
            RecruitmentStatusFilter status,
            Long cursorId,
            int size
    );
}
