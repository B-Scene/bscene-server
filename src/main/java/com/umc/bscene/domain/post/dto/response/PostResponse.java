package com.umc.bscene.domain.post.dto.response;

import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostMedia;
import com.umc.bscene.domain.post.entity.PostTag;
import com.umc.bscene.domain.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long postId,
        Long bandId,
        String bandName,
        PostType type,
        String title,
        String description,
        List<String> mediaUrls,
        List<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getBand().getId(),
                post.getBand().getName(),
                post.getType(),
                post.getTitle(),
                post.getDescription(),
                post.getMediaList().stream().map(PostMedia::getMediaUrl).toList(),
                post.getTagList().stream().map(PostTag::getTagName).toList(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
