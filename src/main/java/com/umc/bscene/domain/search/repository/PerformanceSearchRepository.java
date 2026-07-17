package com.umc.bscene.domain.search.repository;

import com.umc.bscene.domain.search.document.PerformanceDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

// ES performances 인덱스에 대한 저장/삭제
public interface PerformanceSearchRepository extends ElasticsearchRepository<PerformanceDocument, Long> {

    // 밴드 정보(이름 등) 변경 시 연쇄 재색인을 위해 해당 밴드의 공연 문서를 제거할 때 사용
    void deleteByBandId(Long bandId);
}
