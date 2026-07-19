package com.umc.bscene.domain.search.controller;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse;
import com.umc.bscene.domain.search.enums.SearchSortType;
import com.umc.bscene.domain.search.enums.SearchType;
import com.umc.bscene.domain.search.response.code.SearchSuccessCode;
import com.umc.bscene.domain.search.service.SearchService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // 탐색 통합검색 API (검색어 필수, 콘텐츠/정렬/장르/지역 필터 선택, 단일 타입은 커서 기반 무한스크롤)
    @GetMapping("/explore/search")
    public ResponseEntity<SuccessResponse<ExploreSearchResponse>> search(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "ALL") SearchType type,
            @RequestParam(defaultValue = "ACCURACY") SearchSortType sort,
            @RequestParam(required = false) Genre genre,
            @RequestParam(required = false) Region region,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        ExploreSearchResponse response = searchService.search(
                authMember.getUser().getId(), keyword, type, sort, genre, region, cursor, size);
        SuccessResponse<ExploreSearchResponse> successResponse = SuccessResponse.of(
                response,
                SearchSuccessCode.SEARCH_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
