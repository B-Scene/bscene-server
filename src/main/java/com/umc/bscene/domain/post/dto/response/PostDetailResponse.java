package com.umc.bscene.domain.post.dto.response;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostMedia;
import com.umc.bscene.domain.post.entity.PostTag;
import com.umc.bscene.domain.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

// 팬모드 게시물 상세페이지 조회 응답 (댓글 목록은 별도 API : GET /posts/{postId}/comments)
public record PostDetailResponse(
        Long postId,
        WriterBand band,                    // 상단 밴드 정보 + 하단 "밴드 프로필 보기" 버튼용
        PostType type,                      // PHOTO / TEXT / VIDEO — 프론트가 미디어 영역 렌더링 결정
        List<String> mediaUrls,             // 미디어 URL 목록 (사진은 등록 순서, 영상은 1개, 글은 빈 배열)
        String thumbnailUrl,                // 영상 썸네일 (사진/글은 null)
        String title,
        String description,
        List<String> tags,
        LocalDateTime createdAt,
        long likeCount,
        long commentCount,                  // 차단 여부와 관계없이 전체 댓글 수
        boolean isLiked                     // 요청한 사용자의 좋아요(하트) 여부
) {

    // 게시물을 등록한 밴드 정보
    public record WriterBand(
            Long bandId,
            String name,
            String profileImageUrl
    ) {
    }

    public static PostDetailResponse of(Post post, long likeCount, long commentCount, boolean isLiked) {
        Band band = post.getBand();

        return new PostDetailResponse(
                post.getId(),
                new WriterBand(band.getId(), band.getName(), band.getProfileImageUrl()),
                post.getType(),
                post.getMediaList().stream().map(PostMedia::getMediaUrl).toList(),
                post.getThumbnailUrl(),
                post.getTitle(),
                post.getDescription(),
                post.getTagList().stream().map(PostTag::getTagName).toList(),
                post.getCreatedAt(),
                likeCount,
                commentCount,
                isLiked
        );
    }
}
