package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.converter.SessionGenreFormat;
import com.umc.bscene.domain.session.converter.SessionRegionFormat;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.AvailableActivity;
import com.umc.bscene.domain.session.enums.PortfolioMediaType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonPropertyOrder({
        "hasApplication",
        "sessionApplicationId",
        "userId",
        "nickname",
        "title",
        "purpose",
        "oneLineIntro",
        "profileImageUrl",
        "isPublic",
        "part",
        "skillLevel",
        "genre",
        "region",
        "intro",
        "portfolioLinks"
})
public class MySessionApplicationResponse {

    private Boolean hasApplication;

    private Long sessionApplicationId;
    private Long userId;
    private String nickname;
    private String title;
    private String purpose;
    private String oneLineIntro;
    private String profileImageUrl;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isPublic;

    private Part part;
    private SkillLevel skillLevel;
    @SessionGenreFormat
    private Genre genre;
    @SessionRegionFormat
    private Region region;

    private String intro;
    private List<AvailableActivity> availableActivities;
    private List<CareerResponse> careers;

    private List<PortfolioLinkResponse> portfolioLinks;

    public static MySessionApplicationResponse empty(Long userId) {
        return MySessionApplicationResponse.builder()
                .hasApplication(false)
                .sessionApplicationId(null)
                .userId(userId)
                .nickname(null)
                .title(null)
                .purpose(null)
                .oneLineIntro(null)
                .profileImageUrl(null)
                .isPublic(null)
                .part(null)
                .skillLevel(null)
                .genre(null)
                .region(null)
                .intro(null)
                .availableActivities(List.of())
                .careers(List.of())
                .portfolioLinks(List.of())
                .build();
    }

    public static MySessionApplicationResponse from(
            SessionApplication sessionApplication,
            String sessionProfileImageUrl
    ) {
        return from(sessionApplication, sessionProfileImageUrl, true);
    }

    public static MySessionApplicationResponse fromWithoutVisibility(
            SessionApplication sessionApplication,
            String sessionProfileImageUrl
    ) {
        return from(sessionApplication, sessionProfileImageUrl, false);
    }

    private static MySessionApplicationResponse from(
            SessionApplication sessionApplication,
            String sessionProfileImageUrl,
            boolean includeVisibility
    ) {
        return MySessionApplicationResponse.builder()
                .hasApplication(true)
                .sessionApplicationId(sessionApplication.getSessionApplicationId())
                .userId(sessionApplication.getUserId())
                .nickname(sessionApplication.getNickname())
                .title(sessionApplication.getTitle())
                .purpose(sessionApplication.getPurpose())
                .oneLineIntro(sessionApplication.getOneLineIntro())
                .profileImageUrl(sessionProfileImageUrl)
                .isPublic(includeVisibility ? sessionApplication.getIsPublic() : null)
                .part(sessionApplication.getPart())
                .skillLevel(sessionApplication.getSkillLevel())
                .genre(sessionApplication.getGenre())
                .region(sessionApplication.getRegion())
                .intro(sessionApplication.getIntro())
                .availableActivities(List.copyOf(sessionApplication.getAvailableActivities()))
                .careers(sessionApplication.getCareers().stream()
                        .map(CareerResponse::from)
                        .toList())
                .portfolioLinks(
                        sessionApplication.getPortfolioLinks().stream()
                                .filter(link -> link.getDeletedAt() == null)
                                .map(link -> PortfolioLinkResponse.builder()
                                        .sessionApplicationLinkId(link.getSessionApplicationLinkId())
                                        .url(link.getUrl())
                                        .title(link.getTitle())
                                        .thumbnailUrl(link.getThumbnailUrl())
                                        .mediaType(link.getMediaType())
                                        .build())
                                .toList()
                )
                .build();
    }

    @Getter
    @Builder
    @JsonPropertyOrder({
            "sessionApplicationLinkId",
            "url",
            "title",
            "thumbnailUrl",
            "mediaType"
    })
    public static class PortfolioLinkResponse {

        private Long sessionApplicationLinkId;
        private String url;
        private String title;
        private String thumbnailUrl;
        private PortfolioMediaType mediaType;
    }

    public record CareerResponse(
            Long sessionApplicationCareerId,
            String name,
            String period,
            String description
    ) {
        private static CareerResponse from(
                com.umc.bscene.domain.session.entity.SessionApplicationCareer career
        ) {
            return new CareerResponse(career.getSessionApplicationCareerId(), career.getName(),
                    career.getPeriod(), career.getDescription());
        }
    }
}
