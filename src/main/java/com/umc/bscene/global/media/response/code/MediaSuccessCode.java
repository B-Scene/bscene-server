package com.umc.bscene.global.media.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum MediaSuccessCode implements BaseResponseCode {

    PRESIGNED_URL_CREATE_SUCCESS(OK, "MEDIA200_1", "업로드 URL을 발급했어요.");

    private final int status;
    private final String code;
    private final String message;
}
