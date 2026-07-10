package com.umc.bscene.domain.performance.dto.request;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record PerformanceCreateRequest(
        @NotBlank String title,
        @NotNull Genre genre,
        @NotNull LocalDate performanceDate,
        LocalTime startTime,
        String region,
        @NotBlank String venue,
        String description,
        Integer ticketPrice,
        String ticketLink,
        @NotBlank String posterImageUrl
) {
}
