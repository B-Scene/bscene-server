package com.umc.bscene.domain.session.dto.recruitment.response;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Getter
@Builder
@JsonPropertyOrder({
        "sessionRecruitmentId",
        "bandId",
        "deadlineAt",
        "dDay",
        "isNew",
        "isInterested",
        "recruitmentTitle",
        "bandName",
        "bandGenre",
        "bandRegion",
        "summary",
        "part",
        "skillLevel",
        "practiceSchedule"
})
public class SessionRecruitmentListItemResponse {

    private Long sessionRecruitmentId;
    private Long bandId;
    private String recruitmentTitle;
    private String bandName;
    private String bandGenre;
    private String bandRegion;
    private String summary;
    private Part part;
    private SkillLevel skillLevel;
    private String practiceSchedule;
    private LocalDateTime deadlineAt;
    private Long dDay;
    private Boolean isNew;
    private Boolean isInterested;
}
