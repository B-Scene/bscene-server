package com.umc.bscene.domain.search.repository;

import com.umc.bscene.domain.search.document.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

// ES posts 인덱스에 대한 저장/삭제
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, Long> {

    // 밴드 삭제 시 소속 게시물 문서 일괄 제거
    void deleteByBandId(Long bandId);
}
