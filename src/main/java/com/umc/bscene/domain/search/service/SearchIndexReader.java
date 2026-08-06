package com.umc.bscene.domain.search.service;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.search.document.BandDocument;
import com.umc.bscene.domain.search.document.PerformanceDocument;
import com.umc.bscene.domain.search.document.PostDocument;
import com.umc.bscene.domain.search.port.BandPort;
import com.umc.bscene.domain.search.port.FollowPort;
import com.umc.bscene.domain.search.port.PerformancePort;
import com.umc.bscene.domain.search.port.PostPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 색인용 데이터를 MySQL에서 읽어 검색 문서로 변환하는 읽기 전담 빈.
 * ES 반영(SearchIndexService)과 분리한 이유 :
 * - @Transactional은 메서드가 끝날 때까지 DB 커넥션을 점유한다
 *   → ES HTTP 호출까지 한 트랜잭션에 묶으면 ES가 느릴 때 커넥션 풀 고갈로 이어진다
 * - 같은 클래스 내부 호출은 프록시를 거치지 않아 @Transactional이 무시되므로 별도 빈이어야 한다
 * 엔티티가 아닌 완성된 Document를 반환한다 — LAZY 필드 접근(문서 변환)이
 * 영속성 컨텍스트가 살아있는 트랜잭션 안에서 끝나야 하기 때문.
 */
@Component
@RequiredArgsConstructor
public class SearchIndexReader {

    private final BandPort bandPort;
    private final PerformancePort performancePort;
    private final PostPort postPort;
    private final FollowPort followPort;

    // 전체 재색인용 스냅숏 : 세 인덱스에 부을 문서 전체
    public record ReindexData(
            List<BandDocument> bands,
            List<PerformanceDocument> performances,
            List<PostDocument> posts
    ) {
    }

    // 밴드 연쇄 재색인용 스냅숏 : 밴드 문서 + 밴드 정보를 비정규화해 담는 공연·게시물 문서
    public record BandCascadeData(
            BandDocument band,
            List<PerformanceDocument> performances,
            List<PostDocument> posts
    ) {
    }

    // 전체 재색인용 : popularity 집계 → 전체 조회 → 문서 변환을 하나의 읽기 트랜잭션으로 끝낸다
    @Transactional(readOnly = true)
    public ReindexData loadAllForReindex() {
        // 인기순 popularity 집계 (GROUP BY 각 1번) — 전체 재색인이 popularity의 하루 1회 갱신 배치를 겸한다
        Map<Long, Long> followerCounts = followPort.countFollowersGroupedByBand();
        Map<Long, Long> interestCounts = performancePort.countInterestsGroupedByPerformance();

        List<BandDocument> bands = bandPort.findAllForIndexing().stream()
                .map(band -> BandDocument.from(band, followerCounts.getOrDefault(band.getId(), 0L)))
                .toList();

        List<PerformanceDocument> performances = performancePort.findAllActiveWithBand().stream()
                .map(performance -> PerformanceDocument.from(
                        performance, interestCounts.getOrDefault(performance.getId(), 0L)))
                .toList();

        List<PostDocument> posts = postPort.findAllWithBandAndTags().stream()
                .map(post -> PostDocument.from(
                        post, followerCounts.getOrDefault(post.getBand().getId(), 0L)))
                .toList();

        return new ReindexData(bands, performances, posts);
    }

    // 밴드 단건 색인용 : 밴드가 없으면 empty (호출부가 문서 삭제로 처리)
    @Transactional(readOnly = true)
    public Optional<BandCascadeData> loadBandCascade(Long bandId) {
        return bandPort.findById(bandId).map(band -> {
            // 밴드 팔로워 수는 밴드 문서와 소속 게시물 문서가 같은 값을 공유
            long followerCount = followPort.countFollowers(bandId);
            BandDocument bandDocument = BandDocument.from(band, followerCount);

            List<Performance> activePerformances = performancePort.findAllActiveByBandIdWithBand(bandId);
            Map<Long, Long> interestCounts = performancePort.countInterestsByPerformanceIds(
                    activePerformances.stream().map(Performance::getId).toList());
            List<PerformanceDocument> performances = activePerformances.stream()
                    .map(performance -> PerformanceDocument.from(
                            performance, interestCounts.getOrDefault(performance.getId(), 0L)))
                    .toList();

            List<PostDocument> posts = postPort.findAllByBandIdWithBandAndTags(bandId).stream()
                    .map(post -> PostDocument.from(post, followerCount))
                    .toList();

            return new BandCascadeData(bandDocument, performances, posts);
        });
    }

    // 공연 단건 색인용 : ACTIVE가 아니면(삭제·소프트삭제) empty (호출부가 문서 삭제로 처리)
    @Transactional(readOnly = true)
    public Optional<PerformanceDocument> loadPerformanceDocument(Long performanceId) {
        return performancePort.findActiveByIdWithBand(performanceId)
                .map(performance -> PerformanceDocument.from(
                        performance, performancePort.countInterests(performanceId)));
    }

    // 게시물 단건 색인용 : 없으면(삭제됨) empty (호출부가 문서 삭제로 처리)
    @Transactional(readOnly = true)
    public Optional<PostDocument> loadPostDocument(Long postId) {
        return postPort.findByIdWithBandAndTags(postId)
                .map(post -> PostDocument.from(
                        post, followPort.countFollowers(post.getBand().getId())));
    }
}
