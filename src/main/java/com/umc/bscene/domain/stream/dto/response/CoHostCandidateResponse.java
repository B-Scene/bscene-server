package com.umc.bscene.domain.stream.dto.response;

import com.umc.bscene.domain.stream.enums.StreamMemberStatus;

/** 라이브 예약 편집 화면에서 보여줄 공동 진행 후보(같은 밴드의 밴드 멤버)입니다. status가 null이면 미선택 상태입니다. */
public record CoHostCandidateResponse(
        Long userId,
        Long bandMemberId,
        Long bandMemberProfileId,
        String bandMemberProfileImageUrl,
        String nickname,
        String part,
        StreamMemberStatus status   // OWNER | APPROVED | INVITED | REJECTED | null(미선택)
) {
}
