package com.umc.bscene.domain.auth.dto.request;

public record LoginRequest(
        String loginId,
        String password
) {
}