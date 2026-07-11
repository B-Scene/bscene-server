package com.umc.bscene.domain.performance.dto.request;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record PerformanceCreateRequest(
        @NotBlank String title,
        @NotNull Genre genre,
        @NotNull LocalDate performanceDate,
        @NotNull LocalTime startTime,
        @NotNull Region region,
        @NotBlank String venue,
        @NotNull String description,
        @NotNull Integer ticketPrice,
        String ticketLink,
        String posterImageUrl
) {
}
