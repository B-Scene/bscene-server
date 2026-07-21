package com.umc.bscene.domain.user.dto.response;

import com.umc.bscene.domain.user.enums.HistoryYearFilter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 관심 공연 목록 조회 응답 (offset 기반 무한스크롤, 날짜/시간 빠른 순, 연도 필터)
public record InterestedPerformanceResponse(
        Long totalCount,                        // 총 관심 공연 수 (연도 필터 적용, 첫 페이지에만 내려감 / 이후 null)
        HistoryYearFilter appliedFilter,        // 적용된 연도 필터 (ALL/THIS_YEAR/LAST_YEAR/BEFORE)
        int baseYear,                           // 서버 기준 올해 → 프론트가 탭 라벨 계산 (올해/작년/재작년 이전)
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
