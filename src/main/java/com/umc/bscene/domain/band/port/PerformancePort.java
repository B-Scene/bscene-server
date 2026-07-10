package com.umc.bscene.domain.band.port;

public interface PerformancePort {
    /**
     * 밴드의 공연 수를 조회합니다.
     *
     * @param bandId 공연 수를 조회할 밴드 ID
     * @return 해당 밴드의 공연 수
     */
    Long countPerformancesByBandId(Long bandId);
}