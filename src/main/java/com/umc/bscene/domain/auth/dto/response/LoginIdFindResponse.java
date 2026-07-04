package com.umc.bscene.domain.auth.dto.response;

import lombok.Builder;

@Builder
public record LoginIdFindResponse(
        String loginId
) {
}