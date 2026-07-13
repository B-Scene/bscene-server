package com.umc.bscene.domain.stream.dto.response;

import com.umc.bscene.domain.session.enums.Part;

import java.util.List;

/**
 * 라이브 방 멤버(송출자 + 공동 진행자) 프로필 요약입니다.
 * 유저-밴드가 다대다 관계라 밴드마다 다른 예명을 쓸 수 있으므로,
 * 라이브가 생성 시점에 확정한 밴드에 연결된 밴드 멤버 프로필 기준입니다.
 */
public record LiveMemberProfileResponse(
        String bandProfileImageUrl,
        String nickname,
        String bandName,
        List<Part> part,
        Boolean isLeader
) {
}
