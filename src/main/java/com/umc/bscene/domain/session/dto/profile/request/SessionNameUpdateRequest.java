package com.umc.bscene.domain.session.dto.profile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionNameUpdateRequest(
        @NotBlank
        @Size(max = 10)
        String name
) {
}
