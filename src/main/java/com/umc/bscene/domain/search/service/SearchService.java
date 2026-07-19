package com.umc.bscene.domain.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.search.document.BandDocument;
import com.umc.bscene.domain.search.document.PerformanceDocument;
import com.umc.bscene.domain.search.document.VideoDocument;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse.BandItem;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse.PerformanceItem;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse.SearchSection;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse.VideoItem;
import com.umc.bscene.domain.search.enums.SearchType;
import com.umc.bscene.domain.search.exception.SearchException;
import com.umc.bscene.domain.search.port.FollowPort;
import com.umc.bscene.domain.search.response.code.SearchErrorCode;
import com.umc.bscene.domain.search.util.SearchCursor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 탐색 통합검색.
 * - 검색어(must, multi_match)는 필수, 장르·지역(filter, term)은 선택
 * - 정렬 : 정확도(_score) → 최신순(날짜) → docId — 결정적 정렬이라 search_after 커서의 전제가 된다
 * - should 가점 : 구문 일치(match_phrase) + 완전 일치(term, raw) 문서를 상위로
 * - ALL : 섹션별 상위 SECTION_SIZE개 (multiSearch 한 번)
 * - 단일 타입 : search_after 커서 기반 무한스크롤 (size+1개 조회로 hasNext 판정)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int SECTION_SIZE = 3;      // 통합 모드 섹션별 표시 개수
    private static final int MAX_PAGE_SIZE = 30;    // 단일 모드 페이지 크기 상한

    // 타입별 검색 대상 필드와 가중치 (제목·이름 > 밴드명·태그 > 장소·설명)
    // 오타 허용(fuzziness)은 제목·밴드명·장소에만 적용 —
    // 태그(의도적 키워드라 정확성이 생명)·설명(어휘가 많아 오매칭 증가)은 정확 매칭만
    private static final List<String> BAND_FUZZY_FIELDS = List.of("name^3");
    private static final List<String> BAND_EXACT_FIELDS = List.of("description");
    private static final List<String> PERFORMANCE_FUZZY_FIELDS = List.of("title^3", "bandName^2", "venue");
    private static final List<String> PERFORMANCE_EXACT_FIELDS = List.of("description");
    private static final List<String> VIDEO_FUZZY_FIELDS = List.of("title^3", "bandName^2");
    private static final List<String> VIDEO_EXACT_FIELDS = List.of("tags^2", "description");

    // 접두어(부분 입력) 검색 : "블루" → 블루문. edge_ngram(2~10자)으로 색인된 서브필드 (가중치 없음 — 완성어 매칭보다 항상 아래)
    private static final List<String> BAND_PREFIX_FIELDS = List.of("name.prefix");
    private static final List<String> PERFORMANCE_PREFIX_FIELDS = List.of("title.prefix", "bandName.prefix");
    private static final List<String> VIDEO_PREFIX_FIELDS = List.of("title.prefix", "bandName.prefix");

    private final ElasticsearchOperations elasticsearchOperations;
    private final FollowPort followPort;

    public ExploreSearchResponse search(
            Long userId, String keyword, SearchType type,
            Genre genre, Region region, String cursor, int size
    ) {
        if (keyword == null || keyword.isBlank()) {
            throw new SearchException(SearchErrorCode.KEYWORD_REQUIRED);
        }
        String trimmed = keyword.trim();

        try {
            return switch (type) {
                case ALL -> searchAll(userId, trimmed, genre, region);
                case BAND -> searchBandsOnly(userId, trimmed, genre, region, cursor, size);
                case PERFORMANCE -> searchPerformancesOnly(trimmed, genre, region, cursor, size);
                case VIDEO -> searchVideosOnly(trimmed, genre, region, cursor, size);
            };
        } catch (SearchException e) {
            throw e;    // 잘못된 커서(400) 등 의도된 예외는 그대로 전달
        } catch (RuntimeException e) {
            // ES 연결 실패 등 검색 인프라 장애를 503으로 격리 (검색만 실패하고 서비스는 유지)
            log.error("검색 실패 - keyword: {}, type: {}", trimmed, type, e);
            throw new SearchException(SearchErrorCode.SEARCH_UNAVAILABLE);
        }
    }

    // 통합 모드 : 쿼리 3개를 multiSearch(_msearch) 한 번으로 실행, 섹션별 상위 4개 + 전체 건수 합산
    private ExploreSearchResponse searchAll(Long userId, String keyword, Genre genre, Region region) {
        NativeQuery bandQuery = buildQuery(keyword, BAND_FUZZY_FIELDS, BAND_EXACT_FIELDS, BAND_PREFIX_FIELDS,
                "name", genre, region, "createdAt", SECTION_SIZE, null);
        NativeQuery performanceQuery = buildQuery(keyword, PERFORMANCE_FUZZY_FIELDS, PERFORMANCE_EXACT_FIELDS, PERFORMANCE_PREFIX_FIELDS,
                "title", genre, region, "performanceDate", SECTION_SIZE, null);
        NativeQuery videoQuery = buildQuery(keyword, VIDEO_FUZZY_FIELDS, VIDEO_EXACT_FIELDS, VIDEO_PREFIX_FIELDS,
                "title", genre, region, "uploadedAt", SECTION_SIZE, null);

        List<SearchHits<?>> results = elasticsearchOperations.multiSearch(
                List.of(bandQuery, performanceQuery, videoQuery),
                List.of(BandDocument.class, PerformanceDocument.class, VideoDocument.class)
        );

        @SuppressWarnings("unchecked")
        SearchHits<BandDocument> bandHits = (SearchHits<BandDocument>) results.get(0);
        @SuppressWarnings("unchecked")
        SearchHits<PerformanceDocument> performanceHits = (SearchHits<PerformanceDocument>) results.get(1);
        @SuppressWarnings("unchecked")
        SearchHits<VideoDocument> videoHits = (SearchHits<VideoDocument>) results.get(2);

        long totalCount = bandHits.getTotalHits() + performanceHits.getTotalHits() + videoHits.getTotalHits();

        return new ExploreSearchResponse(
                totalCount,
                SearchType.ALL,
                toBandSection(userId, contentsOf(bandHits.getSearchHits()), bandHits.getTotalHits()),
                toPerformanceSection(contentsOf(performanceHits.getSearchHits()), performanceHits.getTotalHits()),
                toVideoSection(contentsOf(videoHits.getSearchHits()), videoHits.getTotalHits()),
                null,
                null
        );
    }

    // 단일 모드 : 밴드만 커서 기반 무한스크롤
    private ExploreSearchResponse searchBandsOnly(Long userId, String keyword, Genre genre, Region region, String cursor, int size) {
        int pageSize = clampSize(size);
        NativeQuery query = buildQuery(keyword, BAND_FUZZY_FIELDS, BAND_EXACT_FIELDS, BAND_PREFIX_FIELDS,
                "name", genre, region, "createdAt", pageSize + 1, SearchCursor.decode(cursor));
        CursorSlice<BandDocument> slice = searchSlice(query, BandDocument.class, pageSize);

        return new ExploreSearchResponse(
                slice.totalHits(), SearchType.BAND,
                toBandSection(userId, slice.contents(), slice.totalHits()), null, null,
                slice.nextCursor(), slice.hasNext()
        );
    }

    // 단일 모드 : 공연만 커서 기반 무한스크롤
    private ExploreSearchResponse searchPerformancesOnly(String keyword, Genre genre, Region region, String cursor, int size) {
        int pageSize = clampSize(size);
        NativeQuery query = buildQuery(keyword, PERFORMANCE_FUZZY_FIELDS, PERFORMANCE_EXACT_FIELDS, PERFORMANCE_PREFIX_FIELDS,
                "title", genre, region, "performanceDate", pageSize + 1, SearchCursor.decode(cursor));
        CursorSlice<PerformanceDocument> slice = searchSlice(query, PerformanceDocument.class, pageSize);

        return new ExploreSearchResponse(
                slice.totalHits(), SearchType.PERFORMANCE,
                null, toPerformanceSection(slice.contents(), slice.totalHits()), null,
                slice.nextCursor(), slice.hasNext()
        );
    }

    // 단일 모드 : 영상만 커서 기반 무한스크롤
    private ExploreSearchResponse searchVideosOnly(String keyword, Genre genre, Region region, String cursor, int size) {
        int pageSize = clampSize(size);
        NativeQuery query = buildQuery(keyword, VIDEO_FUZZY_FIELDS, VIDEO_EXACT_FIELDS, VIDEO_PREFIX_FIELDS,
                "title", genre, region, "uploadedAt", pageSize + 1, SearchCursor.decode(cursor));
        CursorSlice<VideoDocument> slice = searchSlice(query, VideoDocument.class, pageSize);

        return new ExploreSearchResponse(
                slice.totalHits(), SearchType.VIDEO,
                null, null, toVideoSection(slice.contents(), slice.totalHits()),
                slice.nextCursor(), slice.hasNext()
        );
    }

    /**
     * 타입 공통 쿼리 조립 (Kibana 실습에서 검증한 최종 쿼리의 자바 번역).
     * must   : multi_match — 검색어를 타입별 필드·가중치로 매칭 (점수 계산)
     * should : match_phrase(구문 일치) + term(raw 완전 일치) 가점 → 더 비슷한 제목이 위로
     * filter : 장르·지역 선택 시에만 추가 (점수 무관, 캐싱)
     * sort   : _score → 날짜 최신순 → docId — search_after가 요구하는 결정적 정렬
     */
    private NativeQuery buildQuery(
            String keyword, List<String> fuzzyFields, List<String> exactFields, List<String> prefixFields,
            String titleField, Genre genre, Region region, String dateSortField,
            int size, List<Object> searchAfter
    ) {
        var builder = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    // must : 세 조 중 하나라도 매칭되면 결과 포함 (minimum_should_match 1)
                    //  - 오타 허용 조 : 제목·밴드명·장소. AUTO = 길이별 편집거리(2자 이하 0, 3~5자 1, 6자+ 2),
                    //    prefixLength 1 = 첫 글자 정확 일치 강제 (텀 사전 스캔 비용 절감 + 오매칭 방지)
                    //  - 정확 매칭 조 : 태그·설명 (오타 허용 시 오매칭이 이득보다 큼)
                    //  - 접두어 조 : 부분 입력("블루" → 블루문). 오타 허용 없음 (조각 텀에 fuzzy까지 걸면 오매칭 폭발)
                    b.must(m -> m.bool(inner -> inner
                            .should(s -> s.multiMatch(mm -> mm.query(keyword).fields(fuzzyFields)
                                    .fuzziness("AUTO").prefixLength(1)))
                            .should(s -> s.multiMatch(mm -> mm.query(keyword).fields(exactFields)))
                            .should(s -> s.multiMatch(mm -> mm.query(keyword).fields(prefixFields)))
                            .minimumShouldMatch("1")));
                    b.should(s -> s.matchPhrase(mp -> mp.field(titleField).query(keyword)));
                    b.should(s -> s.term(t -> t.field(titleField + ".raw").value(keyword)));
                    if (genre != null) {
                        b.filter(f -> f.term(t -> t.field("genre").value(genre.name())));
                    }
                    if (region != null) {
                        b.filter(f -> f.term(t -> t.field("region").value(region.name())));
                    }
                    return b;
                }))
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field(dateSortField).order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("docId").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(0, size))
                .withTrackTotalHits(true);  // "검색 결과 N개" 표시용 total을 1만 건 상한 없이 정확하게

        if (searchAfter != null) {
            builder.withSearchAfter(searchAfter);   // 커서 지점 다음부터 조회
        }
        return builder.build();
    }

    // size+1개를 조회해 hasNext를 판정하고, 마지막 문서의 정렬값으로 다음 커서를 만든다
    private <T> CursorSlice<T> searchSlice(NativeQuery query, Class<T> documentClass, int pageSize) {
        SearchHits<T> hits = elasticsearchOperations.search(query, documentClass);
        List<SearchHit<T>> searchHits = hits.getSearchHits();

        boolean hasNext = searchHits.size() > pageSize;
        List<SearchHit<T>> pageHits = hasNext ? searchHits.subList(0, pageSize) : searchHits;
        String nextCursor = hasNext
                ? SearchCursor.encode(pageHits.get(pageHits.size() - 1).getSortValues())
                : null;

        return new CursorSlice<>(hits.getTotalHits(), contentsOf(pageHits), hasNext, nextCursor);
    }

    private record CursorSlice<T>(long totalHits, List<T> contents, boolean hasNext, String nextCursor) {
    }

    // 밴드 섹션 조립 : ES 결과(무엇이 검색됐나) + MySQL 팔로우 여부(나와의 관계) 하이브리드
    private SearchSection<BandItem> toBandSection(Long userId, List<BandDocument> documents, long totalHits) {
        List<Long> bandIds = documents.stream().map(BandDocument::getId).toList();
        Set<Long> followingBandIds = Set.copyOf(followPort.findFollowingBandIds(userId, bandIds));

        List<BandItem> items = documents.stream()
                .map(document -> BandItem.from(document, followingBandIds))
                .toList();
        return new SearchSection<>(totalHits, items);
    }

    private SearchSection<PerformanceItem> toPerformanceSection(List<PerformanceDocument> documents, long totalHits) {
        return new SearchSection<>(totalHits, documents.stream().map(PerformanceItem::from).toList());
    }

    private SearchSection<VideoItem> toVideoSection(List<VideoDocument> documents, long totalHits) {
        return new SearchSection<>(totalHits, documents.stream().map(VideoItem::from).toList());
    }

    private static <T> List<T> contentsOf(List<SearchHit<T>> searchHits) {
        return searchHits.stream().map(SearchHit::getContent).toList();
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
