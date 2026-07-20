package com.umc.bscene.domain.user.dto.response.mypage;

import com.umc.bscene.domain.user.enums.UserMode;

import java.util.List;

public record BandMyPageResponse(
        String nickname,
        String bandName,
        List<String> parts,
        UserMode currentMode,
        Long follower,
        Long applicant,
        Long performanceCount,
        Boolean isBandMember
) implements MyPageResponse {
}
