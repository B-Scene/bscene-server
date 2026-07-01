package com.umc.bscene.domain.auth.dto.request;

import com.umc.bscene.domain.user.enums.Gender;

public record SignupRequest(
        String loginId,
        String password,
        String passwordConfirm,
        String name,
        Gender gender,
        String phone
) {
}