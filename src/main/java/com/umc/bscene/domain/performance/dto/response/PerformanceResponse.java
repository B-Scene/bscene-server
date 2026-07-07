package com.umc.bscene.domain.performance.dto.response;

import com.umc.bscene.domain.performance.entity.Performance;

import java.time.LocalDate;
import java.time.LocalTime;

public record PerformanceResponse(
        Long performanceId,
        String title,
        LocalDate performanceDate,
        LocalTime startTime,
        String region,
        String venue,
        String description,
        Integer ticketPrice,
        String ticketLink,
        String posterImageUrl
) {
    public static PerformanceResponse from(Performance performance) {
        return new PerformanceResponse(
                performance.getId(),
                performance.getTitle(),
                performance.getPerformanceDate(),
                performance.getStartTime(),
                performance.getRegion(),
                performance.getVenue(),
                performance.getDescription(),
                performance.getTicketPrice(),
                performance.getTicketLink(),
                performance.getPosterImageUrl()
        );
    }
}
