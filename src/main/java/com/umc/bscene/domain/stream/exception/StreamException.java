package com.umc.bscene.domain.stream.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class StreamException extends BaseException {
    public StreamException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
