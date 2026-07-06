package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MtxAuthRequest(
        String user,    // 클라이언트가 채우는 값, 신뢰하지 않으므로 검증 X

        @NotBlank(message = "password 필드는 필수 값입니다.")
        String password,


        String ip,

        @NotBlank(message = "action 필드는 필수 값입니다.")
        @Pattern(regexp = "publish|read|playback|api|metrics|pprof",
                message = "action 필드 양식이 잘못되었습니다.")
        String action,

        @Pattern(regexp = "^[a-zA-Z0-9\\-]{0,64}$",
                message = "path 필드 양식이 잘못되었습니다.")
        String path,

        String protocol,
        String query
) {
}
