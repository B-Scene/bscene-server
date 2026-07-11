package com.umc.bscene.domain.stream.port;

import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BandMemberPort {

    /**
     * 현재 라이브 전체 조회(커서)에서 밴드 이름과 밴드 프로필 이미지 URL을 전송받기 위한 메소드입니다.
     * @param broadcasterIds 오디오 송출자의 ID를 Set으로 전달합니다.
     * @return BandInfoForGetLiveResponse를 반환해주세요.
     */
    List<BandInfoForGetLiveResponse> getBandNameWithBandProfileByBroadcasterId(Set<Long> broadcasterIds);

    /**
     * 라이브 예약/시작 푸시 알림 발송을 위해, 송출자가 활성화된 밴드의 ID와 이름을 전송받기 위한 메소드입니다.
     * @param broadcasterId 오디오 송출자의 ID를 전달합니다.
     * @return 송출자의 활성 밴드 BandSummaryResponse를 Optional로 반환해주세요. (활성 밴드 없으면 empty)
     */
    Optional<BandSummaryResponse> getBandSummaryByBroadcasterId(Long broadcasterId);

    Optional<BandSummaryResponse> getBandSummaryByBandId(Long bandId);
}
