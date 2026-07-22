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
import com.umc.bscene.domain.session.enums.PortfolioMediaType;
import com.umc.bscene.domain.session.enums.SkillLevel;

import java.time.LocalDateTime;
import java.util.List;

@JsonPropertyOrder({
        "modifiedAt", "profileImageUrl", "name", "defaultPart", "defaultSkillLevel", "defaultRegion",
        "purpose", "title", "oneLineIntro", "intro", "part", "skillLevel",
        "genre", "region", "availableActivities", "careers", "portfolioLinks"
})
public record MySessionApplicationDetailResponse(
        LocalDateTime modifiedAt,
        String profileImageUrl,
        String name,
        Part defaultPart,
        SkillLevel defaultSkillLevel,
        @SessionRegionFormat Region defaultRegion,
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
    public static MySessionApplicationDetailResponse of(
            SessionApplication application,
            String profileImageUrl,
            String name,
            SessionApplication defaultApplication
    ) {
        boolean modified = application.getUpdatedAt() != null
                && application.getCreatedAt() != null
                && application.getUpdatedAt().isAfter(application.getCreatedAt());

        return new MySessionApplicationDetailResponse(
                modified ? application.getUpdatedAt() : null,
                profileImageUrl,
                name,
                defaultApplication == null ? null : defaultApplication.getPart(),
                defaultApplication == null ? null : defaultApplication.getSkillLevel(),
                defaultApplication == null ? null : defaultApplication.getRegion(),
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

    @JsonPropertyOrder({"url", "title", "thumbnailUrl", "mediaType"})
    public record PortfolioLinkResponse(
            String url,
            String title,
            String thumbnailUrl,
            PortfolioMediaType mediaType
    ) {
        private static PortfolioLinkResponse from(SessionApplicationLink link) {
            return new PortfolioLinkResponse(
                    link.getUrl(), link.getTitle(), link.getThumbnailUrl(), link.getMediaType()
            );
        }
    }
}
