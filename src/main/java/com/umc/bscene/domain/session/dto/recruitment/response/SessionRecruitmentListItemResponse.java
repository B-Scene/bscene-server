package com.umc.bscene.domain.session.dto.recruitment.response;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Getter
@Builder
@JsonPropertyOrder({
        "sessionRecruitmentId",
        "bandId",
        "recruitmentTitle",
        "bandName",
        "part",
        "genre",
        "region",
        "deadlineAt",
        "dDay"
})
public class SessionRecruitmentListItemResponse {

    private Long sessionRecruitmentId;
    private Long bandId;
    private String recruitmentTitle;
    private String bandName;
    private Part part;
    private SessionGenre genre;
    private SessionRegion region;
    private LocalDateTime deadlineAt;
    private Long dDay;
}