package com.umc.bscene.domain.session.dto.application.request;

import com.umc.bscene.domain.session.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Getter
@NoArgsConstructor
public class MySessionApplicationCreateRequest {
    @NotBlank(message = "지원서 제목은 필수입니다.")
    @Size(max = 30, message = "지원서 제목은 30자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "지원서 용도는 필수입니다.")
    @Size(max = 20, message = "지원서 용도는 20자 이하여야 합니다.")
    private String purpose;

    @NotBlank(message = "세션 한줄소개는 필수입니다.")
    @Size(max = 100, message = "세션 한줄소개는 100자 이하여야 합니다.")
    private String oneLineIntro;

    @NotBlank(message = "세션 소개글은 필수입니다.")
    @Size(max = 500, message = "세션 소개글은 500자 이하여야 합니다.")
    private String intro;

    @NotNull(message = "지원서 공개 여부는 필수입니다.")
    private Boolean isPublic;
    @NotNull(message = "세션 파트는 필수입니다.")
    private Part part;
    @NotNull(message = "실력대는 필수입니다.")
    private SkillLevel skillLevel;
    @NotNull(message = "선호 장르는 필수입니다.")
    private SessionGenre genre;
    @NotNull(message = "활동 지역은 필수입니다.")
    private SessionRegion region;

    @NotEmpty(message = "가능한 활동을 하나 이상 선택해주세요.")
    private List<@NotNull AvailableActivity> availableActivities;

    @Valid
    private List<CareerRequest> careers;
    @Valid
    private List<PortfolioLinkRequest> portfolioLinks;

    @Getter
    @NoArgsConstructor
    public static class CareerRequest {
        @NotBlank(message = "경력명은 필수입니다.")
        @Size(max = 100, message = "경력명은 100자 이하여야 합니다.")
        private String name;
        @NotBlank(message = "경력 기간은 필수입니다.")
        @Size(max = 100, message = "경력 기간은 100자 이하여야 합니다.")
        private String period;
        @Size(max = 500, message = "경력 상세내용은 500자 이하여야 합니다.")
        private String description;
    }

    @Getter
    @NoArgsConstructor
    public static class PortfolioLinkRequest {
        @NotBlank(message = "포트폴리오 URL은 비어 있을 수 없습니다.")
        @URL(message = "올바른 URL 형식이어야 합니다.")
        @Size(max = 500, message = "포트폴리오 URL은 500자 이하여야 합니다.")
        private String url;
    }
}
