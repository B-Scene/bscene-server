package com.umc.bscene.domain.performance.dto.response;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceParticipation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 참여 확인 대기 공연 목록 응답 (팬모드 홈 '공연은 어떠셨나요?' 모달용, 시작시간 빠른 순)
public record PendingParticipationResponse(
        List<PendingParticipationItem> items
) {
    public static PendingParticipationResponse from(List<PerformanceParticipation> participations) {
        return new PendingParticipationResponse(participations.stream()
                .map(PendingParticipationItem::from)
                .toList());
    }

    // 참여 확인 대기 공연 아이템
    public record PendingParticipationItem(
            Long performanceId,
            String title,
            String venue,
            LocalDate performanceDate,
            LocalTime startTime,
            String posterImageUrl
    ) {
        private static PendingParticipationItem from(PerformanceParticipation participation) {
            Performance performance = participation.getPerformance();
            return new PendingParticipationItem(
                    performance.getId(),
                    performance.getTitle(),
                    performance.getVenue(),
                    performance.getPerformanceDate(),
                    performance.getStartTime(),
                    performance.getPosterImageUrl()
            );
        }
    }
}
