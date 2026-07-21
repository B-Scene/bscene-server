package com.umc.bscene.domain.notification.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class NotificationException extends BaseException {

    public NotificationException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}