package com.umc.bscene.domain.oauth.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class OauthException extends BaseException {

    public OauthException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
