package com.umc.bscene.domain.search.service;

import com.umc.bscene.domain.search.dto.response.RecentSearchListResponse;
import com.umc.bscene.domain.search.dto.response.RecentSearchListResponse.RecentSearchItem;
import com.umc.bscene.domain.search.entity.FanModeRecentSearch;
import com.umc.bscene.domain.search.exception.SearchException;
import com.umc.bscene.domain.search.repository.FanModeRecentSearchRepository;
import com.umc.bscene.domain.search.response.code.SearchErrorCode;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 팬모드 최근 검색어. 유저당 최대 MAX_RECENT_SEARCHES개 —
 * 초과분 삭제는 유일한 저장 경로인 record()에서만 지키면 되므로 애플리케이션 레벨로 관리.
 */
@Service
@RequiredArgsConstructor
public class RecentSearchService {

    private static final int MAX_RECENT_SEARCHES = 10;
    private static final int MAX_KEYWORD_LENGTH = 100;    // keyword 컬럼 varchar(100) 방어

    private final FanModeRecentSearchRepository fanModeRecentSearchRepository;
    private final UserRepository userRepository;

    // 검색 API 성공 시 호출 — 같은 검색어는 searchedAt만 갱신(목록 맨 위로), 새 검색어는 저장 후 초과분 삭제
    @Transactional
    public void record(Long userId, String keyword) {
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            return;
        }
        fanModeRecentSearchRepository.findByUser_IdAndKeyword(userId, keyword).ifPresentOrElse(
                recentSearch -> recentSearch.updateSearchedAt(LocalDateTime.now()),
                () -> {
                    fanModeRecentSearchRepository.save(FanModeRecentSearch.builder()
                            // 검증 없이 FK만 필요하므로 SELECT 없는 프록시 참조 (인증 통과한 userId라 존재 보장)
                            .user(userRepository.getReferenceById(userId))
                            .keyword(keyword)
                            .searchedAt(LocalDateTime.now())
                            .build());
                    trimToLimit(userId);
                }
        );
    }

    @Transactional(readOnly = true)
    public RecentSearchListResponse getRecentSearches(Long userId) {
        List<RecentSearchItem> items = fanModeRecentSearchRepository
                .findAllByUser_IdOrderBySearchedAtDescIdDesc(userId).stream()
                // 동시 검색 경합으로 순간적으로 10개를 넘게 저장돼도 응답은 항상 최대 10개 보장
                .limit(MAX_RECENT_SEARCHES)
                .map(recentSearch -> new RecentSearchItem(recentSearch.getId(), recentSearch.getKeyword()))
                .toList();
        return new RecentSearchListResponse(items);
    }

    // 본인 소유 검색어만 조회되므로 남의 id는 404 (소유권 검증 겸용)
    @Transactional
    public void delete(Long userId, Long recentSearchId) {
        FanModeRecentSearch recentSearch = fanModeRecentSearchRepository
                .findByIdAndUser_Id(recentSearchId, userId)
                .orElseThrow(() -> new SearchException(SearchErrorCode.RECENT_SEARCH_NOT_FOUND));
        fanModeRecentSearchRepository.delete(recentSearch);
    }

    @Transactional
    public void deleteAll(Long userId) {
        fanModeRecentSearchRepository.deleteAllByUser_Id(userId);
    }

    private void trimToLimit(Long userId) {
        List<FanModeRecentSearch> recentSearches =
                fanModeRecentSearchRepository.findAllByUser_IdOrderBySearchedAtDescIdDesc(userId);
        if (recentSearches.size() > MAX_RECENT_SEARCHES) {
            fanModeRecentSearchRepository.deleteAll(
                    recentSearches.subList(MAX_RECENT_SEARCHES, recentSearches.size()));
        }
    }
}
