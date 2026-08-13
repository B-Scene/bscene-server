package com.umc.bscene.domain.stream.dto.request;

import com.umc.bscene.domain.stream.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record ReportUserRequest(

        @NotNull(message = "targetUserId 필드는 필수 값입니다.")
        Long targetUserId,

        // enum 필드는 Jackson 역직렬화 단계에서 허용 값 검증이 이뤄지므로 @Pattern 불필요 (@Pattern은 문자열 전용이라 enum에 붙이면 런타임 예외)
        @NotNull(message = "reportType 필드는 필수 값입니다.")
        ReportType reportType,

        // 신고 대상이 작성한 문제 채팅 내용 (채팅은 서버에 저장되지 않으므로 클라이언트가 신고 시점에 전달).
        // ReportHistory.chatMessage가 NOT NULL이므로 요청 단계에서 필수로 강제 (누락 시 DB 제약 위반 500 방지)
        @NotBlank(message = "chatMessage 필드는 필수 값입니다.")
        @Length(max = 500, message = "chatMessage 필드는 최대 500자까지 작성할 수 있습니다.")
        String chatMessage,

        @Length(max = 500, message = "comment 필드는 최대 500자까지 작성할 수 있습니다.")
        String comment
) {
}
