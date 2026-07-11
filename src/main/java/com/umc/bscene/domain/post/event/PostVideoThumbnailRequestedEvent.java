package com.umc.bscene.domain.post.event;

public record PostVideoThumbnailRequestedEvent(
        Long postId,
        String videoUrl
) {
}
