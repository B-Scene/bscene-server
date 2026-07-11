package com.umc.bscene.domain.stream.port;

import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.CoHostCandidateResponse;

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

    /**
     * 라이브 예약 편집 화면에서 보여줄 공동 진행 후보 리스트를 조회하는 메소드입니다.
     * 송출자의 현재 활성화된 밴드 멤버 프로필이 속한 밴드를 기준으로,
     * 그 밴드에 속한 다른 멤버들(송출자 본인 제외)을 반환합니다.
     * @param broadcasterId 오디오 송출자의 ID를 전달합니다.
     * @return 공동 진행 후보 리스트를 반환해주세요. 송출자의 밴드 멤버 프로필이 없으면 빈 리스트를 반환합니다.
     */
    List<CoHostCandidateResponse> getCoHostCandidatesByBroadcasterId(Long broadcasterId);

    /**
     * 요청자가 송출자(라이브 생성자)가 속한 밴드의 밴드 멤버인지 검사하는 메소드입니다.
     * 세션 멤버는 리소스 접근 제한이므로 false를 반환합니다.
     * @param broadcasterId 오디오 송출자의 ID를 전달합니다.
     * @param userId 검사할 요청자의 ID를 전달합니다.
     * @return 정회원 여부를 반환해주세요.
     */
    boolean isRegularMemberOfBroadcasterBand(Long broadcasterId, Long userId);
}
