package com.umc.bscene.domain.session.dto.profile.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionProfile;
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
        "hasProfile",
        "sessionProfileId",
        "userId",
        "nickname",
        "part",
        "skillLevel",
        "genre",
        "region",
        "intro",
        "portfolioLinks"
})
public class MySessionProfileResponse {

    private Boolean hasProfile;

    private Long sessionProfileId;
    private Long userId;
    private String nickname;

    private Part part;
    private SkillLevel skillLevel;
    private SessionGenre genre;
    private SessionRegion region;

    private String intro;

    private List<PortfolioLinkResponse> portfolioLinks;

    public static MySessionProfileResponse empty(Long userId) {
        return MySessionProfileResponse.builder()
                .hasProfile(false)
                .sessionProfileId(null)
                .userId(userId)
                .nickname(null)
                .part(null)
                .skillLevel(null)
                .genre(null)
                .region(null)
                .intro(null)
                .portfolioLinks(List.of())
                .build();
    }

    public static MySessionProfileResponse from(SessionProfile sessionProfile) {
        return MySessionProfileResponse.builder()
                .hasProfile(true)
                .sessionProfileId(sessionProfile.getSessionProfileId())
                .userId(sessionProfile.getUserId())
                .nickname(sessionProfile.getNickname())
                .part(sessionProfile.getPart())
                .skillLevel(sessionProfile.getSkillLevel())
                .genre(sessionProfile.getGenre())
                .region(sessionProfile.getRegion())
                .intro(sessionProfile.getIntro())
                .portfolioLinks(
                        sessionProfile.getPortfolioLinks().stream()
                                .filter(link -> link.getDeletedAt() == null)
                                .map(link -> PortfolioLinkResponse.builder()
                                        .sessionProfileLinkId(link.getSessionProfileLinkId())
                                        .url(link.getUrl())
                                        .build())
                                .toList()
                )
                .build();
    }

    @Getter
    @Builder
    @JsonPropertyOrder({
            "sessionProfileLinkId",
            "url"
    })
    public static class PortfolioLinkResponse {

        private Long sessionProfileLinkId;
        private String url;
    }
}