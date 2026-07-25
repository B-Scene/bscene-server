package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.user.dto.response.session.SessionApplicationStatusResult;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.global.response.CursorPage;

public interface SessionPort {

    Long findBandIdBySessionApplicationSubmission(Long sasId);

    // 밴드 측의 세션 지원 수락(true -> BAND_ACCEPTED)/거절(false -> REJECTED) 처리 후 지원자 userId 반환
    // 본인 지원 건 결정 금지, PENDING일 때만 원자적으로 전이 (경합 시 한 건만 성공)
    SessionApplicationStatusResult decideApplicationSubmission(Long sasId, Long deciderUserId, boolean isApproved);

    // 밴드가 수락한 지원 건에 대한 지원자의 최종 수락(true -> ACCEPTED)/거절(false -> REJECTED) 처리 후 밴드 ID 반환
    // 본인 지원 건만 가능, BAND_ACCEPTED일 때만 원자적으로 전이 (경합 시 한 건만 성공)
    SessionApplicationStatusResult finalizeApplicationSubmission(Long sasId, Long applicantUserId, boolean isAccepted);

    CursorPage<SessionRecruitmentResponse> findRecruitmentsByBandId(
            Long bandId,
            RecruitmentStatusFilter status,
            Long cursorId,
            int size
    );
}
