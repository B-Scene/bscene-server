package com.umc.bscene.domain.session.dto.recruitment.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.converter.SessionRegionFormat;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonPropertyOrder({
        "sessionRecruitmentId",
        "recruitmentTitle",
        "deadlineAt",
        "dDay",
        "isNew",
        "isInterested",

        "bandId",
        "bandName",
        "bandProfileImageUrl",
        "bandGenre",
        "bandRegion",

        "summary",
        "content",

        "part",
        "genre",
        "region",
        "practiceSchedule",
        "practicePlace",

        "qualification"
})
public class SessionRecruitmentDetailResponse {

    private Long sessionRecruitmentId;
    private String recruitmentTitle;
    private LocalDateTime deadlineAt;
    private Long dDay;
    private Boolean isNew;
    private Boolean isInterested;

    private Long bandId;
    private String bandName;
    private String bandProfileImageUrl;
    private String bandGenre;
    private String bandRegion;

    private String summary;
    private String content;

    private String part;
    private String genre;
    @SessionRegionFormat
    private Region region;
    private String practiceSchedule;
    private String practicePlace;

    private String qualification;
}
