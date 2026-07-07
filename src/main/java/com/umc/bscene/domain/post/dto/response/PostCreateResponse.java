package com.umc.bscene.domain.post.dto.response;

import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostMedia;
import com.umc.bscene.domain.post.entity.PostTag;
import com.umc.bscene.domain.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

public record PostCreateResponse(
        Long postId,
        PostType type,
        String title,
        String description,
        List<String> mediaUrls,
        List<String> tags,
        LocalDateTime createdAt
) {
    public static PostCreateResponse from(Post post) {
        return new PostCreateResponse(
                post.getId(),
                post.getType(),
                post.getTitle(),
                post.getDescription(),
                post.getMediaList().stream().map(PostMedia::getMediaUrl).toList(),
                post.getTagList().stream().map(PostTag::getTagName).toList(),
                post.getCreatedAt()
        );
    }
}
