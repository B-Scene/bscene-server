package com.umc.bscene.domain.performance.dto.request;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;

import java.time.LocalDate;
import java.time.LocalTime;

public record PerformanceUpdateRequest(
        String title,
        Genre genre,
        LocalDate performanceDate,
        LocalTime startTime,
        String venue,
        Integer ticketPrice,
        String ticketLink,
        String posterImageUrl
) {
}
