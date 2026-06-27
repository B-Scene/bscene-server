package com.umc.bscene.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum GeneralSuccessCode implements BaseResponseCode {

    SUCCESS_OK(
            OK,
            "COMMON_200",
            "호출에 성공했습니다."
    ),
    SUCCESS_CREATED(
            CREATED,
            "COMMON_201",
            "리소스 생성에 성공했습니다."
    ),
    SUCCESS_ACCEPTED(
            ACCEPTED,
            "COMMON_202",
            "요청이 받아들여졌습니다."
    ),
    SUCCESS_NO_CONTENT(
            NO_CONTENT,
            "COMMON_204",
            "호출에 성공했습니다."
    )
    ;

    private final int status;
    private final String code;
    private final String message;
}
