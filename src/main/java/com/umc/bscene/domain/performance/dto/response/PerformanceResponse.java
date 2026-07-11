package com.umc.bscene.domain.performance.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.performance.entity.Performance;

import java.time.LocalDate;
import java.time.LocalTime;

public record PerformanceResponse(
        Long performanceId,
        String title,
        Genre genre,
        LocalDate performanceDate,
        LocalTime startTime,
        Region region,
        String venue,
        String description,
        Integer ticketPrice,
        String ticketLink,
        String posterImageUrl,
        Long interestCount,
        boolean isInterested
) {
    public static PerformanceResponse of(Performance performance, Long interestCount, boolean isInterested) {
        return new PerformanceResponse(
                performance.getId(),
                performance.getTitle(),
                performance.getGenre(),
                performance.getPerformanceDate(),
                performance.getStartTime(),
                performance.getRegion(),
                performance.getVenue(),
                performance.getDescription(),
                performance.getTicketPrice(),
                performance.getTicketLink(),
                performance.getPosterImageUrl(),
                interestCount,
                isInterested
        );
    }
}
