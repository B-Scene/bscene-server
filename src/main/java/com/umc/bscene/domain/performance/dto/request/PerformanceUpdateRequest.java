package com.umc.bscene.domain.performance.dto.request;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.performance.enums.AgeRating;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PerformanceUpdateRequest(
        String title,
        Genre genre,
        LocalDate performanceDate,
        LocalTime startTime,
        String venue,
        Integer ticketPrice,
        String ticketLink,
        String posterImageUrl,
        AgeRating ageRating,
        List<String> tags
) {
}
