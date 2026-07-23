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
        "isNew",
        "recruitmentTitle",
        "deadlineAt",
        "dDay",

        "content",

        "part",
        "skillLevel",
        "genre",
        "region",
        "practiceSchedule",
        "practicePlace",

        "qualification",

        "bandId",
        "bandName",
        "bandProfileImageUrl",
        "bandGenre",
        "bandRegion"
})
public class SessionRecruitmentDetailResponse {

    private Long sessionRecruitmentId;
    private Boolean isNew;
    private String recruitmentTitle;
    private LocalDateTime deadlineAt;
    private Long dDay;

    private String content;

    private String part;
    private String skillLevel;
    private String genre;
    @SessionRegionFormat
    private Region region;
    private String practiceSchedule;
    private String practicePlace;

    private String qualification;

    private Long bandId;
    private String bandName;
    private String bandProfileImageUrl;
    private String bandGenre;
    private String bandRegion;
}
