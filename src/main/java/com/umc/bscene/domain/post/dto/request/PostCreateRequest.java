package com.umc.bscene.domain.post.dto.request;

import com.umc.bscene.domain.post.enums.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PostCreateRequest(
        @NotNull PostType type,
        @NotBlank String title,
        String description,
        List<String> mediaUrls,
        List<String> tags
) {
}
