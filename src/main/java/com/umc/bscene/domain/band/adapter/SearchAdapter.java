package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.search.port.BandPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * 검색 색인의 BandPort를 band 도메인이 구현하는 어댑터.
 * 검수 통과(ACCEPTED) 밴드만 색인 대상 — PENDING 밴드에 대해 empty를 반환하면
 * SearchIndexService가 해당 밴드의 기존 검색 문서를 삭제한다.
 */
@RequiredArgsConstructor
public class SearchAdapter implements BandPort {

    private final BandRepository bandRepository;

    @Override
    public List<Band> findAllForIndexing() {
        return bandRepository.findAllByStatus(BandStatus.ACCEPTED);
    }

    @Override
    public Optional<Band> findById(Long bandId) {
        return bandRepository.findByIdAndStatus(bandId, BandStatus.ACCEPTED);
    }
}
