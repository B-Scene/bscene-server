package com.umc.bscene.domain.user.dto.response.session;

import java.util.List;

public record ReceiveRecruitmentsResponse(
        List<SessionRecruitmentResponse> nonExpired,
        List<SessionRecruitmentResponse> expired
) {
}
