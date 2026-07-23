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
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonPropertyOrder({
        "hasDefaultApplication",
        "sessionApplicationId",
        "nickname",
        "profileImageUrl",
        "skillLevel",
        "part",
        "genre",
        "region",
        "applicationCount",
        "submissionCount",
        "inProgressCount",
        "applications"
})
public class MySessionApplicationSummaryResponse {

    private Boolean hasDefaultApplication;
    private Long sessionApplicationId;
    private String nickname;
    private String profileImageUrl;
    private SkillLevel skillLevel;
    private Part part;
    @SessionGenreFormat
    private Genre genre;
    @SessionRegionFormat
    private Region region;
    private Long applicationCount;
    private Long submissionCount;
    private Long inProgressCount;
    private List<ApplicationItem> applications;

    public static MySessionApplicationSummaryResponse of(
            SessionApplication defaultApplication,
            String sessionProfileName,
            String sessionProfileImageUrl,
            long applicationCount,
            long submissionCount,
            long inProgressCount,
            List<SessionApplication> applications
    ) {
        List<ApplicationItem> applicationItems = applications.stream()
                .map(ApplicationItem::from)
                .toList();

        if (defaultApplication == null) {
            return MySessionApplicationSummaryResponse.builder()
                    .hasDefaultApplication(false)
                    .nickname(sessionProfileName)
                    .profileImageUrl(sessionProfileImageUrl)
                    .applicationCount(applicationCount)
                    .submissionCount(submissionCount)
                    .inProgressCount(inProgressCount)
                    .applications(applicationItems)
                    .build();
        }

        return MySessionApplicationSummaryResponse.builder()
                .hasDefaultApplication(true)
                .sessionApplicationId(defaultApplication.getSessionApplicationId())
                .nickname(sessionProfileName != null
                        ? sessionProfileName : defaultApplication.getNickname())
                .profileImageUrl(sessionProfileImageUrl != null
                        ? sessionProfileImageUrl : defaultApplication.getProfileImageUrl())
                .skillLevel(defaultApplication.getSkillLevel())
                .part(defaultApplication.getPart())
                .genre(defaultApplication.getGenre())
                .region(defaultApplication.getRegion())
                .applicationCount(applicationCount)
                .submissionCount(submissionCount)
                .inProgressCount(inProgressCount)
                .applications(applicationItems)
                .build();
    }

    @JsonPropertyOrder({
            "sessionApplicationId",
            "displayDate",
            "isModified",
            "isPublic",
            "purpose",
            "title"
    })
    public record ApplicationItem(
            Long sessionApplicationId,
            LocalDateTime displayDate,
            boolean isModified,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Boolean isPublic,
            String purpose,
            String title
    ) {
        private static ApplicationItem from(SessionApplication application) {
            boolean modified = application.getUpdatedAt() != null
                    && application.getCreatedAt() != null
                    && application.getUpdatedAt().isAfter(application.getCreatedAt());
            return new ApplicationItem(
                    application.getSessionApplicationId(),
                    modified ? application.getUpdatedAt() : application.getCreatedAt(),
                    modified,
                    "기본".equals(application.getPurpose())
                            ? application.getIsPublic()
                            : null,
                    application.getPurpose(),
                    application.getTitle()
            );
        }
    }
}
