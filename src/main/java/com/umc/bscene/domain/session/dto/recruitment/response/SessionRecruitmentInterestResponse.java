package com.umc.bscene.domain.session.dto.recruitment.response;

public record SessionRecruitmentInterestResponse(
        Long sessionRecruitmentId,
        boolean isInterested
) {
    public static SessionRecruitmentInterestResponse of(
            Long sessionRecruitmentId,
            boolean interested
    ) {
        return new SessionRecruitmentInterestResponse(sessionRecruitmentId, interested);
    }
}
