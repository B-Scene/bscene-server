package com.umc.bscene.domain.stream.port;

import com.umc.bscene.domain.stream.dto.CoHostCandidateInfo;
import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.LiveMembersResponse.LiveMemberProfileResponse;

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
     * 라이브가 생성 시점에 확정한 밴드를 기준으로, 그 밴드의 정회원 전체(송출자 본인 포함)를 반환합니다.
     * 멤버의 활성 프로필 여부와 무관하게 해당 밴드 멤버십에 연결된 프로필로 조회해주세요.
     * (기존 공동 진행자가 타 밴드 프로필로 전환해도 목록에서 빠지지 않아야 함)
     * @param bandId 라이브가 생성 시점에 확정한 밴드의 ID를 전달합니다.
     * @return 공동 진행 후보 리스트를 반환해주세요. userId는 스트림 도메인의 초대 상태(OWNER/INVITED 등) 계산용이며 응답에는 노출되지 않습니다.
     */
    List<CoHostCandidateInfo> getCoHostCandidatesByBandId(Long bandId);

    /**
     * 요청자의 현재 활성 밴드 멤버 프로필이 해당 밴드(라이브 생성 시 확정된 밴드)의 정회원 소속인지 검사하는 메소드입니다.
     * 타 밴드 프로필로 전환한 상태면 라이브 생성자 본인이라도 false를 반환합니다.
     * @param bandId 라이브가 생성 시점에 확정한 밴드의 ID를 전달합니다.
     * @param userId 검사할 요청자의 ID를 전달합니다.
     * @return 활성 정회원 여부를 반환해주세요.
     */
    boolean isActiveRegularMemberOfBand(Long bandId, Long userId);

    /**
     * 라이브 방 멤버(송출자 + 공동 진행자) 프로필 요약 조회를 위한 메소드입니다.
     * 유저-밴드가 다대다 관계라 밴드마다 다른 예명을 쓸 수 있으므로, 유저의 활성(active) 프로필이 아니라
     * 라이브가 생성 시점에 확정한 밴드(bandId)의 BandMember에 연결된 밴드 멤버 프로필(BandMember.bandMemberProfile)을 기준으로 조회해주세요.
     * @param bandId 라이브가 생성 시점에 확정한 밴드의 ID를 전달합니다.
     * @param userIds 라이브에 StreamMember로 등록된 유저 ID 리스트를 전달합니다. (송출자 포함)
     * @return LiveMemberProfileResponse 리스트를 userIds 순서대로 반환해주세요.
     *         - bandProfileImageUrl: 밴드 프로필 이미지 URL (Band.profileImageUrl)
     *         - nickname: 해당 밴드에서 사용하는 멤버 프로필의 예명
     *         - bandName: 밴드 이름
     *         - part: 해당 밴드 멤버 프로필의 파트 (현재는 프로필당 단일 파트이므로 1개짜리 리스트)
     *         - isLeader: 밴드 리더(Band.owner) 여부
     *         해당 밴드의 멤버 프로필이 없는 유저는 결과에서 제외해주세요.
     */
    List<LiveMemberProfileResponse> getLiveMemberProfiles(Long bandId, List<Long> userIds);

    /**
     * 라이브 알림을 받을 밴드 구성원 ID를 조회합니다.
     *
     * @param bandId 구성원을 조회할 밴드 ID
     * @return ACCEPTED 상태인 밴드 구성원 사용자 ID 목록
     */
    List<Long> getAcceptedMemberUserIds(Long bandId);

    Optional<String> getBandMemberNickname(Long bandId, Long userId);
}
