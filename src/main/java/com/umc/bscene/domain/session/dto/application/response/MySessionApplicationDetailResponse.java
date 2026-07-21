package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.converter.SessionGenreFormat;
import com.umc.bscene.domain.session.converter.SessionRegionFormat;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationCareer;
import com.umc.bscene.domain.session.entity.SessionApplicationLink;
import com.umc.bscene.domain.session.enums.AvailableActivity;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;

import java.util.List;

@JsonPropertyOrder({
        "purpose", "title", "oneLineIntro", "intro", "part", "skillLevel",
        "genre", "region", "availableActivities", "careers", "portfolioLinks"
})
public record MySessionApplicationDetailResponse(
        String purpose,
        String title,
        String oneLineIntro,
        String intro,
        Part part,
        SkillLevel skillLevel,
        @SessionGenreFormat Genre genre,
        @SessionRegionFormat Region region,
        List<AvailableActivity> availableActivities,
        List<CareerResponse> careers,
        List<PortfolioLinkResponse> portfolioLinks
) {
    public static MySessionApplicationDetailResponse from(SessionApplication application) {
        return new MySessionApplicationDetailResponse(
                application.getPurpose(), application.getTitle(),
                application.getOneLineIntro(), application.getIntro(),
                application.getPart(), application.getSkillLevel(),
                application.getGenre(), application.getRegion(),
                List.copyOf(application.getAvailableActivities()),
                application.getCareers().stream().map(CareerResponse::from).toList(),
                application.getPortfolioLinks().stream()
                        .filter(link -> link.getDeletedAt() == null)
                        .map(PortfolioLinkResponse::from)
                        .toList()
        );
    }

    @JsonPropertyOrder({"name", "period", "description"})
    public record CareerResponse(String name, String period, String description) {
        private static CareerResponse from(SessionApplicationCareer career) {
            return new CareerResponse(
                    career.getName(), career.getPeriod(), career.getDescription()
            );
        }
    }

    @JsonPropertyOrder("url")
    public record PortfolioLinkResponse(String url) {
        private static PortfolioLinkResponse from(SessionApplicationLink link) {
            return new PortfolioLinkResponse(link.getUrl());
        }
    }
}
