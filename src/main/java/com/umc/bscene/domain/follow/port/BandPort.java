package com.umc.bscene.domain.follow.port;

/**
 * 팔로우가 밴드 정보를 조회하기 위한 포트 (adapter는 band 도메인이 구현).
 * 팔로우 대상 밴드의 존재 확인과 자기 밴드 팔로우 차단(멤버십 확인)에 사용된다.
 */
public interface BandPort {

    // 밴드 존재 여부 확인
    boolean existsBand(Long bandId);

    // 해당 밴드의 가입 승인(ACCEPTED)된 멤버인지 확인
    boolean isAcceptedMember(Long bandId, Long userId);
}
