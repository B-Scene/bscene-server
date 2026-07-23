package com.umc.bscene.domain.band.port;

import java.util.Optional;

public interface StreamPort {

    /**
     * 밴드가 현재 진행 중(OPEN)인 라이브의 ID를 조회합니다. (팬모드 밴드 상세의 라이브 입장 버튼용)
     *
     * @param bandId 확인할 밴드 ID
     * @return 라이브 중이면 해당 라이브 ID, 아니면 empty
     */
    Optional<Long> findOpenLiveId(Long bandId);
}
