package com.umc.bscene.domain.search.controller;

import com.umc.bscene.domain.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 검색 색인 관리용 내부 API.
 * nginx가 /api/internal 경로의 외부 접근을 차단하므로 서버 내부에서만 호출 가능하다.
 * (로컬 개발 환경에서는 직접 호출 가능)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/search")
public class SearchInternalController {

    private final SearchIndexService searchIndexService;

    // 전체 재색인 트리거 : 초기 색인, 스키마 변경 후 재적재, 색인 유실 복구용
    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAll() {
        searchIndexService.reindexAll();
        return ResponseEntity.ok("reindex completed");
    }
}
