package com.umc.bscene.domain.media.dto.request;

import com.umc.bscene.domain.media.enums.MediaCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MediaPresignedUrlRequest(
        @NotNull MediaCategory category,
        @NotBlank String fileName,
        @NotBlank String contentType
) {
}
