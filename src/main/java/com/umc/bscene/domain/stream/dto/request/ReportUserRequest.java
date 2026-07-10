package com.umc.bscene.domain.stream.dto.request;

import com.umc.bscene.domain.stream.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record ReportUserRequest(

        @NotNull(message = "targetUserId 필드는 필수 값입니다.")
        Long targetUserId,

        @NotNull(message = "reportType 필드는 필수 값입니다.")
        @Pattern(regexp = "SPAM | ABUSE | SEXUAL | VIOLENCE | COPYRIGHT | ETC",
        message = "reportType의 값이 올바르지 않습니다.")
        ReportType reportType,

        @Length(max = 500, message = "comment 필드는 최대 500자까지 작성할 수 있습니다.")
        String comment
) {
}
