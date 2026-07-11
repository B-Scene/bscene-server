package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonPropertyOrder({
        "sessionApplicationId",
        "userId",
        "nickname",
        "profileImageUrl",
        "isPublic",
        "title",
        "purpose",
        "part",
        "skillLevel",
        "genre",
        "region",
        "intro",
        "portfolioLinks"
})
public class SessionApplicationDetailResponse {

    private Long sessionApplicationId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Boolean isPublic;
    private String title;
    private String purpose;
    private Part part;
    private SkillLevel skillLevel;
    private SessionGenre genre;
    private SessionRegion region;
    private String intro;
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
                .profileImageUrl(sessionProfileImageUrl != null
                        ? sessionProfileImageUrl : application.getProfileImageUrl())
                .isPublic(application.getIsPublic())
                .title(application.getTitle())
                .purpose(application.getPurpose())
                .part(application.getPart())
                .skillLevel(application.getSkillLevel())
                .genre(application.getGenre())
                .region(application.getRegion())
                .intro(application.getIntro())
                .portfolioLinks(application.getPortfolioLinks().stream()
                        .filter(link -> link.getDeletedAt() == null)
                        .map(link -> PortfolioLinkResponse.builder()
                                .sessionApplicationLinkId(
                                        link.getSessionApplicationLinkId()
                                )
                                .url(link.getUrl())
                                .build())
                        .toList())
                .build();
    }

    @Getter
    @Builder
    @JsonPropertyOrder({"sessionApplicationLinkId", "url"})
    public static class PortfolioLinkResponse {

        private Long sessionApplicationLinkId;
        private String url;
    }
}
