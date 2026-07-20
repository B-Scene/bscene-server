package com.umc.bscene.domain.session.dto.recruitment.request;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.session.converter.SessionGenreFormat;
import com.umc.bscene.domain.session.converter.SessionRegionFormat;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SessionRecruitmentCreateRequest {
    @NotNull(message = "밴드 프로필 ID는 필수입니다.")
    private Long bandMemberId;

    @NotBlank(message = "공고 상세 소개는 필수입니다.")
    @Size(max = 500, message = "공고 상세 소개는 500자 이하여야 합니다.")
    private String content;

    @NotBlank(message = "공고 한 줄 소개는 필수입니다.")
    @Size(max = 50, message = "공고 한 줄 소개는 50자 이하여야 합니다.")
    private String summary;
    @NotBlank(message = "모집 공고 제목은 필수입니다.")
    @Size(max = 50, message = "모집 공고 제목은 50자 이하여야 합니다.")
    private String recruitmentTitle;

    @NotNull(message = "모집 파트는 필수입니다.")
    private Part part;

    @NotNull(message = "실력대는 필수입니다.")
    private SkillLevel skillLevel;

    @NotNull(message = "장르는 필수입니다.")
    @SessionGenreFormat
    private Genre genre;

    @NotNull(message = "활동 지역은 필수입니다.")
    @SessionRegionFormat
    private Region region;

    @NotBlank(message = "연습 일정은 필수입니다.")
    @Size(max = 50, message = "연습 일정은 50자 이하여야 합니다.")
    private String practiceSchedule;

    @NotBlank(message = "연습 장소는 필수입니다.")
    @Size(max = 50, message = "연습 장소는 50자 이하여야 합니다.")
    private String practicePlace;

    @NotNull(message = "모집 마감일은 필수입니다.")
    private LocalDateTime deadlineAt;

    @NotBlank(message = "지원 자격은 필수입니다.")
    @Size(max = 500, message = "지원 자격은 500자 이하여야 합니다.")
    private String qualification;
}
