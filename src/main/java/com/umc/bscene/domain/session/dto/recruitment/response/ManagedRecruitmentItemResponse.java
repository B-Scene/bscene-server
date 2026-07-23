package com.umc.bscene.domain.session.dto.recruitment.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonPropertyOrder({
        "sessionRecruitmentId", "dDay", "recruitmentTitle",
        "bandName", "bandGenre", "bandRegion", "postedAgo", "summary"
})
public class ManagedRecruitmentItemResponse {
    private Long sessionRecruitmentId;
    private Long dDay;
    private String recruitmentTitle;
    private String bandName;
    private String bandGenre;
    private String bandRegion;
    private Long postedAgo;
    private String summary;
}
