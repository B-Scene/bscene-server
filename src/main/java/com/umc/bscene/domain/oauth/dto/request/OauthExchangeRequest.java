package com.umc.bscene.domain.oauth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 일회성 코드 교환 요청.
 * 프론트가 리다이렉트로 받은 code로 로그인 결과(토큰/signupToken)를 교환한다.
 */
public record OauthExchangeRequest(

        @NotBlank
        String code
) {
}
