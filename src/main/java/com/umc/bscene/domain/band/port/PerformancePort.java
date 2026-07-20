package com.umc.bscene.domain.band.port;

import com.umc.bscene.domain.performance.enums.PerformanceStatus;

public interface PerformancePort {
    /**
     * 밴드의 공연 수를 조회합니다.
     *
     * @param bandId 공연 수를 조회할 밴드 ID
     * @return 해당 밴드의 공연 수
     */
    Long countPerformancesByBandId(Long bandId);

    /**
     * 밴드가 이제껏 공연한 횟수를 조회합니다.
     *
     * @param bandId 공연 횟수를 조회할 밴드 ID
     * @param status ACTIVE
     * @return 현재까지 진행한 공연의 수
     */
    Long countPerformancesByBandIdAndStatus(Long bandId);
}