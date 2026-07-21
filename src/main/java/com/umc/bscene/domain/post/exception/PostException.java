package com.umc.bscene.domain.post.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class PostException extends BaseException {

    public PostException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
