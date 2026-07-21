package com.umc.bscene.domain.search.repository;

import com.umc.bscene.domain.search.document.BandDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

// ES bands 인덱스에 대한 저장/삭제 (JpaRepository의 ES 버전 — save가 곧 색인)
public interface BandSearchRepository extends ElasticsearchRepository<BandDocument, Long> {
}
