package com.umc.bscene.domain.post.dto.request;

import java.util.List;

public record PostUpdateRequest(
        String title,
        String description,
        List<String> mediaUrls,
        String thumbnailUrl,
        List<String> tags
) {
}
