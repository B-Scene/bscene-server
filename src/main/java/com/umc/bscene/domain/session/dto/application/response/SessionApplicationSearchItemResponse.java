package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.converter.SessionGenreFormat;
import com.umc.bscene.domain.session.converter.SessionRegionFormat;
import com.umc.bscene.domain.session.enums.SkillLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonPropertyOrder({
        "sessionApplicationId",
        "userId",
        "nickname",
        "profileImageUrl",
        "skillLevel",
        "part",
        "genre",
        "region",
        "title",
        "oneLineIntro"
})
public class SessionApplicationSearchItemResponse {

    private Long sessionApplicationId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private SkillLevel skillLevel;
    private Part part;
    @SessionGenreFormat
    private Genre genre;
    @SessionRegionFormat
    private Region region;
    private String title;
    private String oneLineIntro;

    public static SessionApplicationSearchItemResponse from(
            SessionApplication application,
            String sessionProfileName,
            String sessionProfileImageUrl
    ) {
        return SessionApplicationSearchItemResponse.builder()
                .sessionApplicationId(application.getSessionApplicationId())
                .userId(application.getUserId())
                .nickname(sessionProfileName != null
                        ? sessionProfileName : application.getNickname())
                .profileImageUrl(sessionProfileImageUrl != null
                        ? sessionProfileImageUrl : application.getProfileImageUrl())
                .skillLevel(application.getSkillLevel())
                .part(application.getPart())
                .genre(application.getGenre())
                .region(application.getRegion())
                .title(application.getTitle())
                .oneLineIntro(application.getOneLineIntro())
                .build();
    }
}
