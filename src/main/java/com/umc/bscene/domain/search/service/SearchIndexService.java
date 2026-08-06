package com.umc.bscene.domain.search.service;

import com.umc.bscene.domain.search.document.BandDocument;
import com.umc.bscene.domain.search.document.PerformanceDocument;
import com.umc.bscene.domain.search.document.PostDocument;
import com.umc.bscene.domain.search.repository.BandSearchRepository;
import com.umc.bscene.domain.search.repository.PerformanceSearchRepository;
import com.umc.bscene.domain.search.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

/**
 * MySQL(원본) → Elasticsearch(검색용 사본) 색인 반영 전담 서비스.
 * DB 읽기·문서 변환은 SearchIndexReader가 읽기 트랜잭션 안에서 끝내고,
 * 여기서는 트랜잭션 없이 ES에만 쓴다 → ES HTTP 호출 동안 DB 커넥션을 점유하지 않고,
 * 롤백 불가능한 ES 작업이 트랜잭션 밖에 있음이 구조로 드러난다.
 * 전체 재색인은 인덱스를 지우고 다시 만든 뒤 ACTIVE 데이터만 다시 붓는다
 * → 소프트 삭제된 데이터의 유령 문서가 원천 제거된다.
 * 재색인 중 몇 초간 검색 결과가 비는 트레이드오프가 있다 (현재 데이터 규모에서 수용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final SearchIndexReader searchIndexReader;

    private final BandSearchRepository bandSearchRepository;
    private final PerformanceSearchRepository performanceSearchRepository;
    private final PostSearchRepository postSearchRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    // 전체 재색인 : 초기 색인, 스키마 변경 후 재적재, 색인 유실 복구에 공용으로 사용
    public void reindexAll() {
        log.info("검색 전체 재색인 시작");

        // DB 커넥션은 이 호출 안에서만 사용되고 반납된다 — 이후 ES 작업은 DB와 무관
        SearchIndexReader.ReindexData data = searchIndexReader.loadAllForReindex();

        recreateIndex(BandDocument.class);
        if (!data.bands().isEmpty()) bandSearchRepository.saveAll(data.bands());

        recreateIndex(PerformanceDocument.class);
        if (!data.performances().isEmpty()) performanceSearchRepository.saveAll(data.performances());

        recreateIndex(PostDocument.class);
        if (!data.posts().isEmpty()) postSearchRepository.saveAll(data.posts());

        log.info("검색 전체 재색인 완료 - bands: {}건, performances: {}건, posts: {}건",
                data.bands().size(), data.performances().size(), data.posts().size());
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
    public void indexBand(Long bandId) {
        searchIndexReader.loadBandCascade(bandId).ifPresentOrElse(
                data -> {
                    bandSearchRepository.save(data.band());
                    if (!data.performances().isEmpty()) performanceSearchRepository.saveAll(data.performances());
                    if (!data.posts().isEmpty()) postSearchRepository.saveAll(data.posts());

                    log.info("밴드 색인 동기화 완료 - bandId: {} (공연 {}건, 게시물 {}건 연쇄)",
                            bandId, data.performances().size(), data.posts().size());
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
    public void indexPerformance(Long performanceId) {
        searchIndexReader.loadPerformanceDocument(performanceId).ifPresentOrElse(
                performanceSearchRepository::save,
                () -> performanceSearchRepository.deleteById(performanceId)
        );
        log.info("공연 색인 동기화 완료 - performanceId: {}", performanceId);
    }

    // 게시물 변경 : 존재하면 덮어쓰기, 없으면(삭제됨) 문서 삭제
    public void indexPost(Long postId) {
        searchIndexReader.loadPostDocument(postId).ifPresentOrElse(
                postSearchRepository::save,
                () -> postSearchRepository.deleteById(postId)
        );
        log.info("게시물 색인 동기화 완료 - postId: {}", postId);
    }
}
