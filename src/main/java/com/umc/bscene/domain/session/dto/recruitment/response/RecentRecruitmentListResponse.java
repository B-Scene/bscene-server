package com.umc.bscene.domain.session.dto.recruitment.response;

import java.util.List;

public record RecentRecruitmentListResponse(
        List<RecentRecruitmentItemResponse> content,
        int size,
        Long nextCursor,
        boolean hasNext
) {
}
