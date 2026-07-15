package com.umc.bscene.domain.search.service;

import com.umc.bscene.domain.search.document.BandDocument;
import com.umc.bscene.domain.search.document.PerformanceDocument;
import com.umc.bscene.domain.search.document.VideoDocument;
import com.umc.bscene.domain.search.port.BandPort;
import com.umc.bscene.domain.search.port.PerformancePort;
import com.umc.bscene.domain.search.port.PostPort;
import com.umc.bscene.domain.search.repository.BandSearchRepository;
import com.umc.bscene.domain.search.repository.PerformanceSearchRepository;
import com.umc.bscene.domain.search.repository.VideoSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private final BandSearchRepository bandSearchRepository;
    private final PerformanceSearchRepository performanceSearchRepository;
    private final VideoSearchRepository videoSearchRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    // 전체 재색인 : 초기 색인, 스키마 변경 후 재적재, 색인 유실 복구에 공용으로 사용
    @Transactional(readOnly = true)
    public void reindexAll() {
        log.info("검색 전체 재색인 시작");

        recreateIndex(BandDocument.class);
        List<BandDocument> bands = bandPort.findAllForIndexing().stream()
                .map(BandDocument::from)
                .toList();
        if (!bands.isEmpty()) bandSearchRepository.saveAll(bands);

        recreateIndex(PerformanceDocument.class);
        List<PerformanceDocument> performances = performancePort.findAllActiveWithBand().stream()
                .map(PerformanceDocument::from)
                .toList();
        if (!performances.isEmpty()) performanceSearchRepository.saveAll(performances);

        recreateIndex(VideoDocument.class);
        List<VideoDocument> videos = postPort.findAllVideosWithBandAndTags().stream()
                .map(VideoDocument::from)
                .toList();
        if (!videos.isEmpty()) videoSearchRepository.saveAll(videos);

        log.info("검색 전체 재색인 완료 - bands: {}건, performances: {}건, videos: {}건",
                bands.size(), performances.size(), videos.size());
    }

    // 인덱스를 지우고 @Setting(애널라이저)·@Field(매핑) 기반으로 재생성
    private void recreateIndex(Class<?> documentClass) {
        IndexOperations indexOps = elasticsearchOperations.indexOps(documentClass);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();
    }
}
