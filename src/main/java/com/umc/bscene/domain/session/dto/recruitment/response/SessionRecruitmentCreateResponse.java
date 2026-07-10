package com.umc.bscene.domain.session.dto.recruitment.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.SessionRegion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonPropertyOrder({
        "sessionRecruitmentId",
        "bandId",
        "recruitmentTitle",
        "content",
        "part",
        "skillLevel",
        "genre",
        "region",
        "practiceSchedule",
        "practicePlace",
        "deadlineAt",
        "qualification",
        "createdAt"
})
public class SessionRecruitmentCreateResponse {

    private Long sessionRecruitmentId;
    private Long bandId;
    private String recruitmentTitle;
    private String content;
    private String part;
    private String skillLevel;
    private String genre;
    private SessionRegion region;
    private String practiceSchedule;
    private String practicePlace;
    private LocalDateTime deadlineAt;
    private String qualification;

    private LocalDateTime createdAt;

    public static SessionRecruitmentCreateResponse from(SessionRecruitment recruitment) {
        return SessionRecruitmentCreateResponse.builder()
                .sessionRecruitmentId(recruitment.getSessionRecruitmentId())
                .bandId(recruitment.getBand().getId())
                .recruitmentTitle(recruitment.getRecruitmentTitle())
                .content(recruitment.getContent())

                // 한글 응답
                .part(recruitment.getPart().getDescription())
                .skillLevel(recruitment.getSkillLevel().getDescription())
                .genre(recruitment.getGenre().getDescription())

                // enum 유지(@JsonValue로 한글 직렬화)
                .region(recruitment.getRegion())

                .practiceSchedule(recruitment.getPracticeSchedule())
                .practicePlace(recruitment.getPracticePlace())
                .deadlineAt(recruitment.getDeadlineAt())
                .qualification(recruitment.getQualification())
                .createdAt(recruitment.getCreatedAt())
                .build();
    }
}