package com.umc.bscene.global.security.exception;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JwtAuthenticationException extends RuntimeException {

    private final BaseResponseCode baseResponseCode;
}