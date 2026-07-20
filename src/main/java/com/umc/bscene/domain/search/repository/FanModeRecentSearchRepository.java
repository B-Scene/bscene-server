package com.umc.bscene.domain.search.repository;

import com.umc.bscene.domain.search.entity.FanModeRecentSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FanModeRecentSearchRepository extends JpaRepository<FanModeRecentSearch, Long> {

    // 재검색 판별용 — (userId, keyword) 유니크라 최대 1건
    Optional<FanModeRecentSearch> findByUser_IdAndKeyword(Long userId, String keyword);

    // 목록 조회 + 10개 초과분 판별 공용 (searchedAt 동률은 id로 결정적 정렬)
    List<FanModeRecentSearch> findAllByUser_IdOrderBySearchedAtDescIdDesc(Long userId);

    // 개별 삭제용 — 본인 검색어만 조회돼 소유권 검증을 겸한다 (남의 id는 404)
    Optional<FanModeRecentSearch> findByIdAndUser_Id(Long id, Long userId);

    void deleteAllByUser_Id(Long userId);
}
