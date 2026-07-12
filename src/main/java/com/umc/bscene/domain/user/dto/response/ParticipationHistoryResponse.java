package com.umc.bscene.domain.user.dto.response;

import com.umc.bscene.domain.user.enums.HistoryYearFilter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 공연 참여 기록 조회 응답 (offset 기반 무한스크롤, 날짜/시간 빠른 순)
public record ParticipationHistoryResponse(
        Long totalCount,                        // 총 참여 공연 수 (전체 참여 완료 수, 첫 페이지에만 내려감 / 이후 null)
        HistoryYearFilter appliedFilter,        // 적용된 연도 필터 (ALL/THIS_YEAR/LAST_YEAR/BEFORE)
        int baseYear,                           // 서버 기준 올해 → 프론트가 탭 라벨 계산 (올해/작년/재작년 이전)
        List<ParticipationHistoryItem> items,
        int page,                               // 현재 페이지 (0-base)
        boolean hasNext                         // 다음 페이지 존재 여부
) {

    // 참여 기록 목록 아이템 (참여 완료한 공연)
    public record ParticipationHistoryItem(
            Long performanceId,
            String title,
            String venue,
            LocalDate performanceDate,
            LocalTime startTime,
            String posterImageUrl,
            String status                       // 참여 상태 (항상 COMPLETED)
    ) {
    }
}
