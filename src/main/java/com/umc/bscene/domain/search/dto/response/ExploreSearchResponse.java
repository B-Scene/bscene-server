package com.umc.bscene.domain.search.dto.response;

import com.umc.bscene.domain.search.document.BandDocument;
import com.umc.bscene.domain.search.document.PerformanceDocument;
import com.umc.bscene.domain.search.document.VideoDocument;
import com.umc.bscene.domain.search.enums.SearchType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * 탐색 통합검색 응답.
 * - ALL(통합 모드) : bands/performances/videos 세 섹션 모두 채움 (섹션별 최대 4개), page/hasNext는 null
 * - 단일 모드 : 해당 타입 섹션만 채우고 나머지는 null, page/hasNext로 무한스크롤
 */
public record ExploreSearchResponse(
        long totalCount,                            // ALL : 세 섹션 합산 / 단일 : 해당 타입 전체 건수
        SearchType type,                            // 적용된 콘텐츠 필터 echo
        SearchSection<BandItem> bands,
        SearchSection<PerformanceItem> performances,
        SearchSection<VideoItem> videos,
        Integer page,                               // 단일 모드에서만 (0-base)
        Boolean hasNext                             // 단일 모드에서만
) {

    // 섹션 : 타입별 총 건수 + 아이템 목록
    public record SearchSection<T>(long totalCount, List<T> items) {
    }

    public record BandItem(
            Long bandId,
            String name,
            String genre,
            String region,
            String description,
            String profileImageUrl,
            boolean isFollowing                     // 사용자별 데이터 — ES가 아닌 MySQL에서 조회 후 조립
    ) {
        public static BandItem from(BandDocument document, Set<Long> followingBandIds) {
            return new BandItem(
                    document.getId(),
                    document.getName(),
                    document.getGenre(),
                    document.getRegion(),
                    document.getDescription(),
                    document.getProfileImageUrl(),
                    followingBandIds.contains(document.getId())
            );
        }
    }

    public record PerformanceItem(
            Long performanceId,
            String title,
            String bandName,
            String venue,
            LocalDate performanceDate,
            LocalTime startTime,
            String genre,
            String region,
            String posterImageUrl
    ) {
        public static PerformanceItem from(PerformanceDocument document) {
            return new PerformanceItem(
                    document.getId(),
                    document.getTitle(),
                    document.getBandName(),
                    document.getVenue(),
                    document.getPerformanceDate(),
                    document.getStartTime(),
                    document.getGenre(),
                    document.getRegion(),
                    document.getPosterImageUrl()
            );
        }
    }

    public record VideoItem(
            Long videoId,
            String title,
            String bandName,
            List<String> tags,
            String thumbnailUrl,
            LocalDateTime uploadedAt
    ) {
        public static VideoItem from(VideoDocument document) {
            return new VideoItem(
                    document.getId(),
                    document.getTitle(),
                    document.getBandName(),
                    document.getTags(),
                    document.getThumbnailUrl(),
                    document.getUploadedAt()
            );
        }
    }
}
