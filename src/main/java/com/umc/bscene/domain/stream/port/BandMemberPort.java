package com.umc.bscene.domain.stream.port;

import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;

import java.util.List;
import java.util.Set;

public interface BandMemberPort {

    /**
     * 현재 라이브 전체 조회(커서)에서 밴드 이름과 밴드 프로필 이미지 URL을 전송받기 위한 메소드입니다.
     * @param broadcasterIds 오디오 송출자의 ID를 Set으로 전달합니다.
     * @return BandInfoForGetLiveResponse를 반환해주세요.
     */
    List<BandInfoForGetLiveResponse> getBandNameWithBandProfileByBroadcasterId(Set<String> broadcasterIds);
}
