package com.umc.bscene.domain.performance.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

public record PerformanceUpdateRequest(
        String title,
        LocalDate performanceDate,
        LocalTime startTime,
        String venue,
        Integer ticketPrice,
        String ticketLink,
        String posterImageUrl
) {
}
