package com.umc.bscene.domain.band.dto.request;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BandCreateRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        Genre genre,

        @NotNull
        Region region,

        String profileImageUrl,

        @Size(max = 1000)
        String description
) {
}
