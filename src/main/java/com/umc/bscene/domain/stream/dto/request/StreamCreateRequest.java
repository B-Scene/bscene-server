package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record StreamCreateRequest(
        @NotBlank(message = "title 필드는 필수 값입니다.")
        String title,

        @Size(max = 100, message = "description 필드는 100자를 초과할 수 없습니다.")
        String description,

        @Future(message = "현재 시각과 일치하거나, 과거인 시간은 예약 시각으로 설정할 수 없습니다.")
        LocalDateTime scheduledAt,
        List<Long> coHost
) {
}
