package com.umc.bscene.domain.session.dto.recruitment.response;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import lombok.Builder;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Getter
@Builder
@JsonPropertyOrder({
        "sessionRecruitmentId",
        "bandId",
        "dDay",
        "isNew",
        "isInterested",
        "recruitmentTitle",
        "bandName",
        "bandGenre",
        "bandRegion",
        "postedAgo",
        "summary",
        "part",
        "skillLevel"
})
public class SessionRecruitmentListItemResponse {

    private Long sessionRecruitmentId;
    private Long bandId;
    private String recruitmentTitle;
    private String bandName;
    private String bandGenre;
    private String bandRegion;
    private Long postedAgo;
    private String summary;
    private Part part;
    private SkillLevel skillLevel;
    private Long dDay;
    private Boolean isNew;
    private Boolean isInterested;
}
