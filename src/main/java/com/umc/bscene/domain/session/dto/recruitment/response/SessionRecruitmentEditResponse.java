package com.umc.bscene.domain.session.dto.recruitment.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonPropertyOrder({
        "recruitmentTitle",
        "summary",
        "content",
        "part",
        "skillLevel",
        "genre",
        "region",
        "practiceSchedule",
        "practicePlace",
        "deadlineAt",
        "qualification"
})
public class SessionRecruitmentEditResponse {
    private String recruitmentTitle;
    private String summary;
    private String content;
    private String part;
    private String skillLevel;
    private String genre;
    private String region;
    private String practiceSchedule;
    private String practicePlace;
    private LocalDateTime deadlineAt;
    private String qualification;
}
