package com.umc.bscene.domain.post.port;

import java.util.Optional;

/**
 * 게시물 댓글이 밴드 멤버십 정보를 조회하기 위한 포트 (adapter는 band 도메인이 구현).
 * 밴드모드 댓글의 자격(멤버십) 검증과 명의(멤버 프로필) 결정에 사용된다.
 */
public interface BandPort {

    // 해당 밴드의 가입 승인(ACCEPTED)된 멤버인지 확인
    boolean isAcceptedMember(Long bandId, Long userId);

    // 해당 밴드 멤버십에 연결된 멤버 프로필 id (멤버가 아니거나 프로필 미연결이면 empty)
    Optional<Long> findMemberProfileId(Long bandId, Long userId);
}
