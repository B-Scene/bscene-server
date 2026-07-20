package com.umc.bscene.domain.search.dto.response;

import java.util.List;

// 팬모드 최근 검색어 목록 (최대 10개, 최근 검색순)
public record RecentSearchListResponse(
        List<RecentSearchItem> recentSearches
) {

    public record RecentSearchItem(
            Long recentSearchId,    // 개별 삭제(X 버튼) 호출에 사용
            String keyword
    ) {
    }
}
