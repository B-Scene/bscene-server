package com.umc.bscene.domain.session.dto.recruitment.request;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SessionRecruitmentUpdateRequest {

    private String content;

    @NotNull(message = "모집 파트는 필수입니다.")
    private Part part;

    @NotNull(message = "실력대는 필수입니다.")
    private SkillLevel skillLevel;

    @NotNull(message = "장르는 필수입니다.")
    private SessionGenre genre;

    @NotNull(message = "활동 지역은 필수입니다.")
    private SessionRegion region;

    @NotNull(message = "연습 일정은 필수입니다.")
    private String practiceSchedule;

    @NotNull(message = "연습 장소는 필수입니다.")
    private String practicePlace;

    @NotNull(message = "모집 마감일은 필수입니다.")
    private LocalDateTime deadlineAt;

    @NotNull(message = "지원 자격은 필수입니다.")
    private String qualification;
}