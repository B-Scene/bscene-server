package com.umc.bscene.domain.session.dto.application.request;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Getter
@NoArgsConstructor
public class MySessionApplicationUpdateRequest {

    @NotBlank(message = "지원서 제목은 필수입니다.")
    @Size(max = 30, message = "지원서 제목은 30자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "지원서 용도는 필수입니다.")
    @Size(max = 20, message = "지원서 용도는 20자 이하여야 합니다.")
    private String purpose;

    @URL(message = "프로필 이미지는 올바른 URL 형식이어야 합니다.")
    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
    private String profileImageUrl;

    @NotNull(message = "세션 파트는 필수입니다.")
    private Part part;

    @NotNull(message = "실력대는 필수입니다.")
    private SkillLevel skillLevel;

    @NotNull(message = "장르는 필수입니다.")
    private SessionGenre genre;

    @NotNull(message = "활동 지역은 필수입니다.")
    private SessionRegion region;

    private String intro;

    @Valid
    private List<PortfolioLinkRequest> portfolioLinks;

    @Getter
    @NoArgsConstructor
    public static class PortfolioLinkRequest {

        @URL(message = "올바른 URL 형식이어야 합니다.")
        private String url;
    }
}
