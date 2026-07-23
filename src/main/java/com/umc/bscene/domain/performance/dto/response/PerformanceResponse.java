package com.umc.bscene.domain.performance.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceTag;
import com.umc.bscene.domain.performance.enums.AgeRating;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PerformanceResponse(
        Long performanceId,
        String title,
        Genre genre,
        LocalDate performanceDate,
        LocalTime startTime,
        Region region,
        String venue,
        String description,
        String ticketPrice,
        String ticketLink,
        String posterImageUrl,
        AgeRating ageRating,
        List<String> tags,
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
                performance.getAgeRating(),
                performance.getTagList().stream().map(PerformanceTag::getTagName).toList(),
                interestCount,
                isInterested
        );
    }
}
