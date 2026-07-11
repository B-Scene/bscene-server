package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonPropertyOrder({
        "hasDefaultApplication",
        "sessionApplicationId",
        "nickname",
        "profileImageUrl",
        "isPublic",
        "skillLevel",
        "part",
        "genre",
        "region",
        "applicationCount",
        "submissionCount",
        "inProgressCount"
})
public class MySessionApplicationSummaryResponse {

    private Boolean hasDefaultApplication;
    private Long sessionApplicationId;
    private String nickname;
    private String profileImageUrl;
    private Boolean isPublic;
    private SkillLevel skillLevel;
    private Part part;
    private SessionGenre genre;
    private SessionRegion region;
    private Long applicationCount;
    private Long submissionCount;
    private Long inProgressCount;

    public static MySessionApplicationSummaryResponse of(
            SessionApplication defaultApplication,
            String sessionProfileName,
            String sessionProfileImageUrl,
            long applicationCount,
            long submissionCount,
            long inProgressCount
    ) {
        if (defaultApplication == null) {
            return MySessionApplicationSummaryResponse.builder()
                    .hasDefaultApplication(false)
                    .nickname(sessionProfileName)
                    .profileImageUrl(sessionProfileImageUrl)
                    .applicationCount(applicationCount)
                    .submissionCount(submissionCount)
                    .inProgressCount(inProgressCount)
                    .build();
        }

        return MySessionApplicationSummaryResponse.builder()
                .hasDefaultApplication(true)
                .sessionApplicationId(defaultApplication.getSessionApplicationId())
                .nickname(sessionProfileName != null
                        ? sessionProfileName : defaultApplication.getNickname())
                .profileImageUrl(sessionProfileImageUrl != null
                        ? sessionProfileImageUrl : defaultApplication.getProfileImageUrl())
                .isPublic(defaultApplication.getIsPublic())
                .skillLevel(defaultApplication.getSkillLevel())
                .part(defaultApplication.getPart())
                .genre(defaultApplication.getGenre())
                .region(defaultApplication.getRegion())
                .applicationCount(applicationCount)
                .submissionCount(submissionCount)
                .inProgressCount(inProgressCount)
                .build();
    }
}
