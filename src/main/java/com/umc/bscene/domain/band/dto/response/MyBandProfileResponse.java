package com.umc.bscene.domain.band.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.band.entity.BandProfile;
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
        "bandProfileId",
        "userId",
        "nickname",
        "part",
        "skillLevel",
        "genre",
        "region",
        "intro",
        "portfolioLinks"
})
public class MyBandProfileResponse {

    private Boolean hasProfile;

    private Long bandProfileId;
    private Long userId;
    private String nickname;

    private Part part;
    private SkillLevel skillLevel;
    private SessionGenre genre;
    private SessionRegion region;

    private String intro;

    private List<PortfolioLinkResponse> portfolioLinks;

    public static MyBandProfileResponse empty(Long userId) {
        return MyBandProfileResponse.builder()
                .hasProfile(false)
                .bandProfileId(null)
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

    public static MyBandProfileResponse from(BandProfile bandProfile) {
        return MyBandProfileResponse.builder()
                .hasProfile(true)
                .bandProfileId(bandProfile.getBandProfileId())
                .userId(bandProfile.getUserId())
                .nickname(bandProfile.getNickname())
                .part(bandProfile.getPart())
                .skillLevel(bandProfile.getSkillLevel())
                .genre(bandProfile.getGenre())
                .region(bandProfile.getRegion())
                .intro(bandProfile.getIntro())
                .portfolioLinks(
                        bandProfile.getPortfolioLinks().stream()
                                .filter(link -> link.getDeletedAt() == null)
                                .map(link -> PortfolioLinkResponse.builder()
                                        .bandProfileLinkId(link.getBandProfileLinkId())
                                        .url(link.getUrl())
                                        .build())
                                .toList()
                )
                .build();
    }

    @Getter
    @Builder
    @JsonPropertyOrder({
            "bandProfileLinkId",
            "url"
    })
    public static class PortfolioLinkResponse {

        private Long bandProfileLinkId;
        private String url;
    }
}