package com.umc.bscene.domain.search.service;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.search.document.BandDocument;
import com.umc.bscene.domain.search.document.PerformanceDocument;
import com.umc.bscene.domain.search.document.PostDocument;
import com.umc.bscene.domain.search.port.BandPort;
import com.umc.bscene.domain.search.port.FollowPort;
import com.umc.bscene.domain.search.port.PerformancePort;
import com.umc.bscene.domain.search.port.PostPort;
import com.umc.bscene.domain.search.repository.BandSearchRepository;
import com.umc.bscene.domain.search.repository.PerformanceSearchRepository;
import com.umc.bscene.domain.search.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * MySQL(원본) → Elasticsearch(검색용 사본) 전체 재색인.
 * 인덱스를 지우고 다시 만든 뒤 ACTIVE 데이터만 다시 붓는다
 * → 소프트 삭제된 데이터의 유령 문서가 원천 제거된다.
 * 재색인 중 몇 초간 검색 결과가 비는 트레이드오프가 있다 (현재 데이터 규모에서 수용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final BandPort bandPort;
    private final PerformancePort performancePort;
    private final PostPort postPort;
    private final FollowPort followPort;

    private final BandSearchRepository bandSearchRepository;
    private final PerformanceSearchRepository performanceSearchRepository;
    private final PostSearchRepository postSearchRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    // 전체 재색인 : 초기 색인, 스키마 변경 후 재적재, 색인 유실 복구에 공용으로 사용
    @Transactional(readOnly = true)
    public void reindexAll() {
        log.info("검색 전체 재색인 시작");

        // 인기순 popularity 집계 (GROUP BY 각 1번) — 이 재색인이 popularity의 하루 1회 갱신 배치를 겸한다
        Map<Long, Long> followerCounts = followPort.countFollowersGroupedByBand();
        Map<Long, Long> interestCounts = performancePort.countInterestsGroupedByPerformance();

        recreateIndex(BandDocument.class);
        List<BandDocument> bands = bandPort.findAllForIndexing().stream()
                .map(band -> BandDocument.from(band, followerCounts.getOrDefault(band.getId(), 0L)))
                .toList();
        if (!bands.isEmpty()) bandSearchRepository.saveAll(bands);

        recreateIndex(PerformanceDocument.class);
        List<PerformanceDocument> performances = performancePort.findAllActiveWithBand().stream()
                .map(performance -> PerformanceDocument.from(
                        performance, interestCounts.getOrDefault(performance.getId(), 0L)))
                .toList();
        if (!performances.isEmpty()) performanceSearchRepository.saveAll(performances);

        recreateIndex(PostDocument.class);
        List<PostDocument> posts = postPort.findAllWithBandAndTags().stream()
                .map(post -> PostDocument.from(
                        post, followerCounts.getOrDefault(post.getBand().getId(), 0L)))
                .toList();
        if (!posts.isEmpty()) postSearchRepository.saveAll(posts);

        log.info("검색 전체 재색인 완료 - bands: {}건, performances: {}건, posts: {}건",
                bands.size(), performances.size(), posts.size());
    }

    // 인덱스를 지우고 @Setting(애널라이저)·@Field(매핑) 기반으로 재생성
    private void recreateIndex(Class<?> documentClass) {
        IndexOperations indexOps = elasticsearchOperations.indexOps(documentClass);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();
    }

    // ===== 이벤트 기반 단건 동기화 =====
    // 공통 원칙 : 원본(MySQL)을 다시 조회해서 있으면 덮어쓰기(멱등), 없으면 문서 삭제

    // 밴드 변경 : 밴드 문서 + 비정규화된 밴드명·장르·지역을 쓰는 공연·게시물 문서까지 연쇄 재색인
    @Transactional(readOnly = true)
    public void indexBand(Long bandId) {
        bandPort.findById(bandId).ifPresentOrElse(
                band -> {
                    // 밴드 팔로워 수는 밴드 문서와 소속 게시물 문서가 같은 값을 공유
                    long followerCount = followPort.countFollowers(bandId);
                    bandSearchRepository.save(BandDocument.from(band, followerCount));

                    List<Performance> activePerformances = performancePort.findAllActiveByBandIdWithBand(bandId);
                    Map<Long, Long> interestCounts = performancePort.countInterestsByPerformanceIds(
                            activePerformances.stream().map(Performance::getId).toList());
                    List<PerformanceDocument> performances = activePerformances.stream()
                            .map(performance -> PerformanceDocument.from(
                                    performance, interestCounts.getOrDefault(performance.getId(), 0L)))
                            .toList();
                    if (!performances.isEmpty()) performanceSearchRepository.saveAll(performances);

                    List<PostDocument> posts = postPort.findAllByBandIdWithBandAndTags(bandId).stream()
                            .map(post -> PostDocument.from(post, followerCount))
                            .toList();
                    if (!posts.isEmpty()) postSearchRepository.saveAll(posts);

                    log.info("밴드 색인 동기화 완료 - bandId: {} (공연 {}건, 게시물 {}건 연쇄)",
                            bandId, performances.size(), posts.size());
                },
                () -> {
                    // 밴드가 사라졌으면 밴드 문서와 소속 공연·게시물 문서를 모두 제거
                    bandSearchRepository.deleteById(bandId);
                    performanceSearchRepository.deleteByBandId(bandId);
                    postSearchRepository.deleteByBandId(bandId);
                    log.info("밴드 색인 삭제 완료 - bandId: {}", bandId);
                }
        );
    }

    // 공연 변경 : ACTIVE면 덮어쓰기, 아니면(삭제·소프트삭제) 문서 삭제
    @Transactional(readOnly = true)
    public void indexPerformance(Long performanceId) {
        performancePort.findActiveByIdWithBand(performanceId).ifPresentOrElse(
                performance -> performanceSearchRepository.save(PerformanceDocument.from(
                        performance, performancePort.countInterests(performanceId))),
                () -> performanceSearchRepository.deleteById(performanceId)
        );
        log.info("공연 색인 동기화 완료 - performanceId: {}", performanceId);
    }

    // 게시물 변경 : 존재하면 덮어쓰기, 없으면(삭제됨) 문서 삭제
    @Transactional(readOnly = true)
    public void indexPost(Long postId) {
        postPort.findByIdWithBandAndTags(postId).ifPresentOrElse(
                post -> postSearchRepository.save(PostDocument.from(
                        post, followPort.countFollowers(post.getBand().getId()))),
                () -> postSearchRepository.deleteById(postId)
        );
        log.info("게시물 색인 동기화 완료 - postId: {}", postId);
    }
}
