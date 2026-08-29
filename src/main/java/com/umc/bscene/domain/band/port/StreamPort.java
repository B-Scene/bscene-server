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

    /**
     * 밴드의 라이브 이력(audio_stream 행) 존재 여부를 조회합니다.
     * audio_stream.band_id는 FK가 아니라서 밴드 삭제 시 DB 제약으로 걸러지지 않으므로,
     * 검수 플로우의 밴드 삭제 안전장치가 애플리케이션 단에서 직접 확인하는 용도.
     * 취소·종료된 라이브도 고아 행이 되긴 마찬가지이므로 상태 무관하게 판단한다.
     *
     * @param bandId 확인할 밴드 ID
     * @return 라이브 이력이 하나라도 있으면 true
     */
    boolean hasLiveHistory(Long bandId);
}
