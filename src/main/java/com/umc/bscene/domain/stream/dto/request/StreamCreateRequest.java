package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;

public record StreamCreateRequest(
        @NotBlank(message = "title 필드는 필수 값입니다.")
        @Length(max = 50, message = "title 필드는 최대 50자 미만이어야 합니다.")
        String title,

        @Size(max = 100, message = "description 필드는 100자를 초과할 수 없습니다.")
        String description,

        // presigned URL 발급(/media/presigned-url) 후 클라이언트가 S3 업로드를 마치고 돌려받은 fileUrl
        @Size(max = 255, message = "thumbnailImageUrl 필드는 255자를 초과할 수 없습니다.")
        String thumbnailImageUrl,

        @Future(message = "현재 시각과 일치하거나, 과거인 시간은 예약 시각으로 설정할 수 없습니다.")
        LocalDateTime scheduledAt,

        // 공동 진행자로 초대할 사용자 ID 목록
        @Size(max = 10, message = "coHost는 최대 10명까지 지정할 수 있습니다.")
        List<@NotNull(message = "coHost 원소는 null일 수 없습니다.") Long> coHost
) {
}
