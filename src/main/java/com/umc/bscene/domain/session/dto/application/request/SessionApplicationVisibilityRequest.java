package com.umc.bscene.domain.session.dto.application.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SessionApplicationVisibilityRequest {

    @NotNull(message = "공개 여부는 필수입니다.")
    private Boolean isPublic;
}
