package com.umc.bscene.domain.session.dto.recruitment.response;

import java.util.List;

public record InterestedRecruitmentListResponse(
        List<InterestedRecruitmentItemResponse> content,
        int size,
        Long nextCursor,
        boolean hasNext
) {
}
