package com.umc.bscene.domain.search.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class SearchException extends BaseException {

    public SearchException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}
