package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public record StreamCreateRequest(
        @NotBlank(message = "title 필드는 필수 값입니다.")
        String title,

        String description,

        @Future
        LocalDateTime scheduledAt,
        List<Long> coHost
) {
}
