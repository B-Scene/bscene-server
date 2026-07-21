package com.umc.bscene.domain.performance.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class PerformanceException extends BaseException {

    public PerformanceException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
