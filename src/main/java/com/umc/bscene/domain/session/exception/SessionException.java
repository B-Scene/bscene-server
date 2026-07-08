package com.umc.bscene.domain.session.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class SessionException extends BaseException {

    public SessionException(BaseResponseCode code) {
        super(code);
    }
}