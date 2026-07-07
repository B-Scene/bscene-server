package com.umc.bscene.domain.performance.adapter;

import com.umc.bscene.domain.band.port.PerformancePort;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BandPerformanceAdapter implements PerformancePort {

    private final PerformanceRepository performanceRepository;

    @Override
    public Long countPerformancesByBandId(Long bandId) {
        return performanceRepository.countByBand_IdAndStatus(
                bandId,
                PerformanceStatus.ACTIVE
        );
    }
}