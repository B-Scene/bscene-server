package com.umc.bscene.domain.fanhome.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

// 팔로우한 밴드 소식 카드 한 개 (Post + 밴드 정보 조합)
public record BandNewsItem(
        Long bandId,
        String bandName,
        String bandProfileImageUrl,
        Genre genre,              // 밴드 장르 (카드 상단 "장르 · 지역")
        Region region,            // 밴드 지역
        Long postId,              // 클릭 시 게시물 상세로 이동
        PostType type,            // 프론트가 사진/영상/텍스트 렌더 분기 (영상이면 재생아이콘)
        String imageUrl,          // 카드 미리보기 이미지 (PHOTO=첫 사진, VIDEO=썸네일(추후), TEXT=null)
        String title,
        String description,
        List<String> tags,        // 게시물 태그 (PostTag)
        LocalDateTime createdAt   // 상대시간("2시간 전")은 프론트가 계산
) {
}
