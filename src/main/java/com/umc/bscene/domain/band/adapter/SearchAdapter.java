package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.search.port.BandPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * 검색 색인의 BandPort를 band 도메인이 구현하는 어댑터.
 */
@RequiredArgsConstructor
public class SearchAdapter implements BandPort {

    private final BandRepository bandRepository;

    @Override
    public List<Band> findAllForIndexing() {
        return bandRepository.findAll();
    }

    @Override
    public Optional<Band> findById(Long bandId) {
        return bandRepository.findById(bandId);
    }
}
