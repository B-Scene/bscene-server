package com.umc.bscene.domain.post.dto.response;

import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.enums.PostType;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long postId,
        PostType type,
        String title,
        String thumbnailUrl,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post) {
        String thumbnailUrl = post.getThumbnailUrl() != null
                ? post.getThumbnailUrl()
                : post.getMediaList().isEmpty() ? null : post.getMediaList().get(0).getMediaUrl();

        return new PostSummaryResponse(
                post.getId(),
                post.getType(),
                post.getTitle(),
                thumbnailUrl,
                post.getCreatedAt()
        );
    }
}
