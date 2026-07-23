package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.user.dto.response.session.ReceiveRecruitmentsResponse;

public interface SessionPort {

    ReceiveRecruitmentsResponse findPendingRecruitmentsByBandId(Long userId, Long bandId);
}
