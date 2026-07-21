package com.umc.bscene.global.media.dto.request;

import com.umc.bscene.global.media.enums.MediaCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MediaPresignedUrlRequest(
        @NotNull MediaCategory category,
        @NotBlank String fileName,
        @NotBlank String contentType
) {
}
