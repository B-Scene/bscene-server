package com.umc.bscene.domain.band.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class BandException extends BaseException {

    public BandException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
