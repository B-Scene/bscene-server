package com.umc.bscene.global.swagger;

import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.response.code.GeneralSuccessCode;

/** 공통 응답의 제네릭을 OpenAPI에 노출하기 위한 문서 전용 타입. */
public abstract class SwaggerResponseEnvelope<T> extends SuccessResponse<T> {

    protected SwaggerResponseEnvelope() {
        super(null, GeneralSuccessCode.SUCCESS_OK);
    }
}
