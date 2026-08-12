package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.user.dto.response.BandMemberResponse;
import com.umc.bscene.domain.user.dto.response.MyBandProfile;
import com.umc.bscene.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface BandPort {

    // 해당 밴드의 정회원(ACCEPTED, MEMBER)인지 + 이 밴드 프로필로 전환(활성화)된 상태인지 검증
    // 미소속 -> BAND_PERMISSION_DENIED / 소속이지만 밴드 모드 아님 -> BAND_MODE_REQUIRED
    void validateActiveBandMember(Long userId, Long bandId);

    // 세션 지원 수락 시 지원자를 세션 멤버(memberType=SESSION)로 등록
    // 지원 시점 지원서의 닉네임·파트로 pre-fill한 멤버 프로필도 함께 생성해 연결
    // 반환값은 생성된 멤버 프로필 PK - 이미 소속이라 등록을 건너뛰면 null
    Long registerSessionMember(Long bandId, User applicant, String nickname, Part part);

    // 활성 밴드 멤버 프로필 조회 - 활성 프로필이 하나도 없으면 empty (밴드 모드 첫 진입 판단은 호출부 책임)
    Optional<BandMemberResponse> getActiveBandMemberProfile(Long userId);
    Long getActiveBandMemberProfile_BandIdIdByUserId(Long userIdl);
    List<MyBandProfile> getAssociatedBandProfiles(Long userId);

    void changeProfileByProfileId(Long userId, Long profileId);

    void deactivateCurrentActiveProfile(Long userId);

    List<Long> getAcceptedMemberUserIds(Long bandId);
}
