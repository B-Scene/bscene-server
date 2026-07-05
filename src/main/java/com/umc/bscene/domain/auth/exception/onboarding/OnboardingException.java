package com.umc.bscene.domain.auth.exception.onboarding;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class OnboardingException extends BaseException {

    public OnboardingException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
