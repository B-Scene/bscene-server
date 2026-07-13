package com.umc.bscene.domain.user.enums;

// 공연 참여 기록 연도 필터 (서버 기준 현재 연도에 상대적 → 해가 바뀌면 자동으로 밀림)
public enum HistoryYearFilter {
    ALL,         // 전체
    THIS_YEAR,   // 올해 (baseYear)
    LAST_YEAR,   // 작년 (baseYear - 1)
    BEFORE       // 재작년 이전 (baseYear - 2 이하)
}
