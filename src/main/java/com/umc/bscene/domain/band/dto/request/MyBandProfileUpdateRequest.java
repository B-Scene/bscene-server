package com.umc.bscene.domain.band.dto.request;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Getter
@NoArgsConstructor
public class MyBandProfileUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 30, message = "닉네임은 최대 30자입니다.")
    private String nickname;

    @NotNull(message = "세션 파트는 필수입니다.")
    private Part part;

    @NotNull(message = "실력대는 필수입니다.")
    private SkillLevel skillLevel;

    @NotNull(message = "장르는 필수입니다.")
    private SessionGenre genre;

    @NotNull(message = "활동 지역은 필수입니다.")
    private SessionRegion region;

    @Size(max = 500, message = "소개는 최대 500자입니다.")
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