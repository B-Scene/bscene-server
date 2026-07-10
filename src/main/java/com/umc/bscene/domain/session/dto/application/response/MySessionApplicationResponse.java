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
        "hasApplication",
        "sessionApplicationId",
        "userId",
        "nickname",
        "title",
        "purpose",
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

    private Part part;
    private SkillLevel skillLevel;
    private SessionGenre genre;
    private SessionRegion region;

    private String intro;

    private List<PortfolioLinkResponse> portfolioLinks;

    public static MySessionApplicationResponse empty(Long userId) {
        return MySessionApplicationResponse.builder()
                .hasApplication(false)
                .sessionApplicationId(null)
                .userId(userId)
                .nickname(null)
                .title(null)
                .purpose(null)
                .part(null)
                .skillLevel(null)
                .genre(null)
                .region(null)
                .intro(null)
                .portfolioLinks(List.of())
                .build();
    }

    public static MySessionApplicationResponse from(SessionApplication sessionApplication) {
        return MySessionApplicationResponse.builder()
                .hasApplication(true)
                .sessionApplicationId(sessionApplication.getSessionApplicationId())
                .userId(sessionApplication.getUserId())
                .nickname(sessionApplication.getNickname())
                .title(sessionApplication.getTitle())
                .purpose(sessionApplication.getPurpose())
                .part(sessionApplication.getPart())
                .skillLevel(sessionApplication.getSkillLevel())
                .genre(sessionApplication.getGenre())
                .region(sessionApplication.getRegion())
                .intro(sessionApplication.getIntro())
                .portfolioLinks(
                        sessionApplication.getPortfolioLinks().stream()
                                .filter(link -> link.getDeletedAt() == null)
                                .map(link -> PortfolioLinkResponse.builder()
                                        .sessionApplicationLinkId(link.getSessionApplicationLinkId())
                                        .url(link.getUrl())
                                        .build())
                                .toList()
                )
                .build();
    }

    @Getter
    @Builder
    @JsonPropertyOrder({
            "sessionApplicationLinkId",
            "url"
    })
    public static class PortfolioLinkResponse {

        private Long sessionApplicationLinkId;
        private String url;
    }
}
