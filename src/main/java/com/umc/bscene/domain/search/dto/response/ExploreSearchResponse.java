package com.umc.bscene.domain.search.dto.response;

import com.umc.bscene.domain.search.document.BandDocument;
import com.umc.bscene.domain.search.document.PerformanceDocument;
import com.umc.bscene.domain.search.document.PostDocument;
import com.umc.bscene.domain.search.enums.SearchType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * 탐색 통합검색 응답.
 * - ALL(통합 모드) : bands/performances/posts 세 섹션 모두 채움 (섹션별 최대 3개), 커서 필드는 null
 * - 단일 모드 : 해당 타입 섹션만 채우고 나머지는 null, nextCursor/hasNext로 커서 기반 무한스크롤
 */
public record ExploreSearchResponse(
        long totalCount,                            // ALL : 세 섹션 합산 / 단일 : 해당 타입 전체 건수
        SearchType type,                            // 적용된 콘텐츠 필터 echo
        SearchSection<BandItem> bands,
        SearchSection<PerformanceItem> performances,
        SearchSection<PostItem> posts,
        String nextCursor,                          // 단일 모드에서만 — 다음 요청에 그대로 전달 (마지막 페이지면 null)
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

    public record PostItem(
            Long postId,        // 게시물(Post)의 PK — 게시물 상세 이동 시 그대로 사용
            String postType,    // VIDEO/PHOTO/TEXT — 카드 표시 분기용
            String title,
            String bandName,
            String bandProfileImageUrl,
            String description,
            List<String> tags,
            String thumbnailUrl,    // VIDEO=썸네일, PHOTO=첫 사진, TEXT=null
            LocalDateTime uploadedAt
    ) {
        public static PostItem from(PostDocument document) {
            return new PostItem(
                    document.getId(),
                    document.getPostType(),
                    document.getTitle(),
                    document.getBandName(),
                    document.getBandProfileImageUrl(),
                    document.getDescription(),
                    document.getTags(),
                    document.getThumbnailUrl(),
                    document.getUploadedAt()
            );
        }
    }
}
