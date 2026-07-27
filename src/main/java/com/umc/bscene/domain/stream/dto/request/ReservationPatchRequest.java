package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 라이브 예약 수정(PATCH) 요청입니다. null인 필드는 수정하지 않고 기존 값을 유지합니다.
 */
public record ReservationPatchRequest(
        @Length(max = 50, message = "title 필드는 최대 50자 이하여야 합니다.")
        // 필수 필드인 제목이 빈 문자열/공백으로 덮이지 않도록 방지 (null은 '변경 없음'이므로 허용)
        @Pattern(regexp = "(?s).*\\S.*", message = "title 필드는 공백만으로 채울 수 없습니다.")
        String title,

        @Size(max = 100, message = "description 필드는 100자를 초과할 수 없습니다.")
        String description,

        String thumbnailImageUrl,

        @Future(message = "현재 시각과 일치하거나, 과거인 시간은 예약 시각으로 설정할 수 없습니다.")
        LocalDateTime scheduledAt,

        // null이면 변경 없음, 빈 리스트면 공동 진행 전체 해제
        // 공동 진행자로 초대할 사용자 ID 목록
        @Size(max = 10, message = "coHost는 최대 10명까지 지정할 수 있습니다.")
        List<@NotNull(message = "coHost 원소는 null일 수 없습니다.") Long> coHost
) {
}
