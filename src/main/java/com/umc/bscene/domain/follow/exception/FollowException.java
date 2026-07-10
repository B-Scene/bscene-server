package com.umc.bscene.domain.follow.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class FollowException extends BaseException {

    public FollowException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
