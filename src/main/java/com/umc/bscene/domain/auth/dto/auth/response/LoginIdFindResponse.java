package com.umc.bscene.domain.auth.dto.auth.response;

import lombok.Builder;

@Builder
public record LoginIdFindResponse(
        String loginId
) {
}