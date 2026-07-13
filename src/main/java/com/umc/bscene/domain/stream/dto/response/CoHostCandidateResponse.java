package com.umc.bscene.domain.stream.dto.response;

/** 라이브 예약 편집 화면에서 보여줄 공동 진행 후보(같은 밴드의 밴드 멤버)입니다. */
public record CoHostCandidateResponse(
        Long userId,
        String name
) {
}
