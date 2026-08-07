package com.umc.bscene.domain.performance.dto.response;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceTag;
import com.umc.bscene.domain.performance.enums.AgeRating;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 팬모드 공연 상세페이지 조회 응답
// 공연정보/공연소개/캐스팅이 한 페이지에 섹션(앵커 이동)으로 그려지는 화면이라 단일 응답으로 전부 내려줌
public record PerformanceDetailResponse(
        Long performanceId,
        String title,
        String genre,                       // 공연 장르 한글명, 예: "하드록"
        LocalDate performanceDate,
        LocalTime startTime,
        String region,                      // 공연 지역 한글명, 예: "서울"
        String venue,
        String description,
        String ticketPrice,
        String ticketLink,
        String posterImageUrl,
        AgeRating ageRating,                // 관람 연령 (ALL/AGE_12/AGE_15/AGE_19)
        List<String> tags,
        Long interestCount,
        boolean isInterested,               // 관심 공연(하트) 등록 여부
        String participationStatus,         // null: 알림 미설정 / SCHEDULED: 알림 설정됨 / COMPLETED: 참여 완료
        List<CastingBand> casting           // 캐스팅 밴드 목록 (베타는 공연 등록 밴드 1팀, 추후 합동 공연 대비 리스트)
) {

    // 캐스팅 섹션 아이템 (카드 클릭 시 밴드 상세로 이동)
    public record CastingBand(
            Long bandId,
            String name,
            String genre,                   // 밴드 장르 한글명
            String region,                  // 밴드 지역 한글명
            String profileImageUrl
    ) {
    }

    public static PerformanceDetailResponse of(
            Performance performance, Long interestCount, boolean isInterested, String participationStatus) {
        Band band = performance.getBand();

        return new PerformanceDetailResponse(
                performance.getId(),
                performance.getTitle(),
                performance.getGenre().getName(),
                performance.getPerformanceDate(),
                performance.getStartTime(),
                performance.getRegion().getName(),
                performance.getVenue(),
                performance.getDescription(),
                performance.getTicketPrice(),
                performance.getTicketLink(),
                performance.getPosterImageUrl(),
                performance.getAgeRating(),
                performance.getTagList().stream().map(PerformanceTag::getTagName).toList(),
                interestCount,
                isInterested,
                participationStatus,
                List.of(new CastingBand(
                        band.getId(),
                        band.getName(),
                        band.getGenre().getName(),
                        band.getRegion().getName(),
                        band.getProfileImageUrl()
                ))
        );
    }
}
