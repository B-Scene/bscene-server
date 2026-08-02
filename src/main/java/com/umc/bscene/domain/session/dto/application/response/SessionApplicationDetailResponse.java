package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
        "sessionApplicationId",
        "title",
        "purpose",

        "userId",
        "nickname",
        "profileImageUrl",
        "isPublic",

        "oneLineIntro",
        "intro",

        "part",
        "skillLevel",
        "genre",
        "region",
        "availableActivities",

        "careers",
        "portfolioLinks"
})
public class SessionApplicationDetailResponse {

    private Long sessionApplicationId;
    private String title;
    private String purpose;

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Boolean isPublic;

    private String oneLineIntro;
    private String intro;

    private Part part;
    private SkillLevel skillLevel;
    @SessionGenreFormat
    private Genre genre;
    @SessionRegionFormat
    private Region region;
    private List<AvailableActivity> availableActivities;

    private List<MySessionApplicationResponse.CareerResponse> careers;
    private List<PortfolioLinkResponse> portfolioLinks;

    public static SessionApplicationDetailResponse from(
            SessionApplication application,
            String sessionProfileName,
            String sessionProfileImageUrl
    ) {
        return SessionApplicationDetailResponse.builder()
                .sessionApplicationId(application.getSessionApplicationId())
                .userId(application.getUserId())
                .nickname(sessionProfileName != null
                        ? sessionProfileName : application.getNickname())
                .profileImageUrl(sessionProfileImageUrl)
                .isPublic(application.getIsPublic())
                .title(application.getTitle())
                .purpose(application.getPurpose())
                .oneLineIntro(application.getOneLineIntro())
                .part(application.getPart())
                .skillLevel(application.getSkillLevel())
                .genre(application.getGenre())
                .region(application.getRegion())
                .intro(application.getIntro())
                .availableActivities(List.copyOf(application.getAvailableActivities()))
                .careers(application.getCareers().stream()
                        .map(career -> new MySessionApplicationResponse.CareerResponse(
                                career.getSessionApplicationCareerId(),
                                career.getName(),
                                career.getPeriod(),
                                career.getDescription()))
                        .toList())
                .portfolioLinks(application.getPortfolioLinks().stream()
                        .filter(link -> link.getDeletedAt() == null)
                        .map(link -> PortfolioLinkResponse.builder()
                                .sessionApplicationLinkId(
                                        link.getSessionApplicationLinkId()
                                )
                                .url(link.getUrl())
                                .title(link.getTitle())
                                .thumbnailUrl(link.getThumbnailUrl())
                                .mediaType(link.getMediaType())
                                .build())
                        .toList())
                .build();
    }

    @Getter
    @Builder
    @JsonPropertyOrder({
            "sessionApplicationLinkId", "url", "title", "thumbnailUrl", "mediaType"
    })
    public static class PortfolioLinkResponse {

        private Long sessionApplicationLinkId;
        private String url;
        private String title;
        private String thumbnailUrl;
        private PortfolioMediaType mediaType;
    }
}
