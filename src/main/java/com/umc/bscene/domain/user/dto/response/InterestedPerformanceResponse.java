package com.umc.bscene.domain.user.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 관심 공연 목록 조회 응답 (offset 기반 무한스크롤, 날짜/시간 빠른 순)
public record InterestedPerformanceResponse(
        List<InterestedPerformanceItem> items,
        int page,                               // 현재 페이지 (0-base)
        boolean hasNext                         // 다음 페이지 존재 여부
) {

    // 관심 공연 목록 아이템
    public record InterestedPerformanceItem(
            Long performanceId,
            String title,
            String venue,
            LocalDate performanceDate,
            LocalTime startTime,
            String posterImageUrl,
            String participationStatus          // null: 알림 미설정 / SCHEDULED: 알림 설정됨(참여완료 버튼 활성) / COMPLETED: 참여 완료
    ) {
    }
}
