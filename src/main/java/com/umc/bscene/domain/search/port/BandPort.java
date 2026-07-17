package com.umc.bscene.domain.search.port;

import com.umc.bscene.domain.band.entity.Band;

import java.util.List;
import java.util.Optional;

/**
 * 검색 색인이 밴드 데이터를 조회하기 위한 포트 (adapter는 band 도메인이 구현).
 */
public interface BandPort {

    // 전체 색인용 : 모든 밴드 조회
    List<Band> findAllForIndexing();

    // 단건 색인용 : 밴드 조회 (없으면 색인 대신 문서 삭제 처리)
    Optional<Band> findById(Long bandId);
}
