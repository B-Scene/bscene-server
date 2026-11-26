package com.umc.bscene.domain.session.dto.profile.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record SessionProfileImageUpdateRequest(
        @NotNull
        @Size(max = 500)
        @URL
        @Pattern(regexp = "(?i)(?:https?://.+)?")
        String profileImageUrl
) {
}
