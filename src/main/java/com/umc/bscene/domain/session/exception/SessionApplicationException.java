package com.umc.bscene.domain.session.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class SessionApplicationException extends BaseException {

    public SessionApplicationException(BaseResponseCode code) {
        super(code);
    }
}
