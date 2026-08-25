package com.umc.bscene.domain.search.controller;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse;
import com.umc.bscene.domain.search.dto.response.RecentSearchListResponse;
import com.umc.bscene.domain.search.enums.SearchSortType;
import com.umc.bscene.domain.search.enums.SearchType;
import com.umc.bscene.domain.search.response.code.SearchSuccessCode;
import com.umc.bscene.domain.search.service.RecentSearchService;
import com.umc.bscene.domain.search.service.SearchService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final RecentSearchService recentSearchService;

    // 탐색 통합검색 API (검색어 필수, 콘텐츠/정렬/장르/지역 필터 선택, 단일 타입은 커서 기반 무한스크롤)
    // withDummy=false면 더미 밴드(와 그 공연·게시물)를 결과에서 제외, 생략·true면 전체 (기존 호환)
    @GetMapping("/explore/search")
    public ResponseEntity<SuccessResponse<ExploreSearchResponse>> search(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "ALL") SearchType type,
            @RequestParam(defaultValue = "ACCURACY") SearchSortType sort,
            @RequestParam(required = false) Genre genre,
            @RequestParam(required = false) Region region,
            @RequestParam(defaultValue = "true") boolean withDummy,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        ExploreSearchResponse response = searchService.search(
                authMember.getUser().getId(), keyword, type, sort, genre, region, withDummy, cursor, size);
        SuccessResponse<ExploreSearchResponse> successResponse = SuccessResponse.of(
                response,
                SearchSuccessCode.SEARCH_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 팬모드 최근 검색어 목록 조회 API (최대 10개, 최근 검색순 — 저장은 검색 API가 자동 처리)
    @GetMapping("/explore/search/recent")
    public ResponseEntity<SuccessResponse<RecentSearchListResponse>> getRecentSearches(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        RecentSearchListResponse response = recentSearchService.getRecentSearches(authMember.getUser().getId());
        SuccessResponse<RecentSearchListResponse> successResponse = SuccessResponse.of(
                response,
                SearchSuccessCode.RECENT_SEARCH_LIST_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 팬모드 최근 검색어 개별 삭제 API
    @DeleteMapping("/explore/search/recent/{recentSearchId}")
    public ResponseEntity<SuccessResponse<Void>> deleteRecentSearch(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long recentSearchId
    ) {
        recentSearchService.delete(authMember.getUser().getId(), recentSearchId);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                SearchSuccessCode.RECENT_SEARCH_DELETE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 팬모드 최근 검색어 전체 삭제 API
    @DeleteMapping("/explore/search/recent")
    public ResponseEntity<SuccessResponse<Void>> deleteAllRecentSearches(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        recentSearchService.deleteAll(authMember.getUser().getId());
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                SearchSuccessCode.RECENT_SEARCH_DELETE_ALL_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
