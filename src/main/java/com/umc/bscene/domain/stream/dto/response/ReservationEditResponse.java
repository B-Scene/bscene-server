package com.umc.bscene.domain.stream.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 라이브 예약 편집 화면 조회 응답입니다.
 * title, description, thumbnailImageUrl, scheduledAt은 pre-fill 용도이고,
 * coHostCandidates는 공동 진행으로 선택 가능한 밴드 멤버 리스트입니다. (현재 초대 상태는 각 후보의 status로 표시)
 */
public record ReservationEditResponse(
        Long liveId,
        String title,
        String description,
        String thumbnailImageUrl,
        LocalDateTime scheduledAt,
        List<CoHostCandidateResponse> coHostCandidates
) {
}
