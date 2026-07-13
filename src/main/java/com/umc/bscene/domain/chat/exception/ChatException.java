package com.umc.bscene.domain.chat.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class ChatException extends BaseException {
    public ChatException(BaseResponseCode code) { super(code); }
}
