package com.umc.bscene.domain.user.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class UserException extends BaseException {

    public UserException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
