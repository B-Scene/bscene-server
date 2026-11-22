package com.umc.bscene.domain.oauth.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.SERVICE_UNAVAILABLE;

@Getter
@RequiredArgsConstructor
public enum AppleOauthErrorCode implements BaseResponseCode {

    AUTH_STORE_UNAVAILABLE(SERVICE_UNAVAILABLE, "COMMON_503_1", "인증 저장소를 일시적으로 사용할 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
