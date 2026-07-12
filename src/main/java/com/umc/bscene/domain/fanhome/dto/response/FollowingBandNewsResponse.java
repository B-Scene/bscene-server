package com.umc.bscene.domain.fanhome.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

// 팔로우한 밴드 소식 전체 조회 응답 (커서 기반 무한스크롤)
public record FollowingBandNewsResponse(
        List<NewsItem> items,
        Long nextCursor,       // 다음 페이지 조회용 커서 (마지막 소식의 postId), 다음 페이지 없으면 null
        boolean hasNext        // 다음 페이지 존재 여부
) {
    // 소식 카드 한 개 (Post + 밴드 정보 조합)
    public record NewsItem(
            Long bandId,
            String bandName,
            String bandProfileImageUrl,
            Genre genre,
            Region region,
            Long postId,
            PostType type,                 // PHOTO | VIDEO | TEXT (프론트 렌더 분기)
            List<String> mediaUrls,        // PHOTO: 사진 전부(sortOrder순) / VIDEO: 썸네일 / TEXT: 빈 배열
            String title,
            String description,
            List<String> tags,
            LocalDateTime createdAt        // 상대시간("2시간 전")은 프론트가 계산
    ) {
    }
}
