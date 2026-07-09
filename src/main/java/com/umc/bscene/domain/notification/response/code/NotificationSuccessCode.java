package com.umc.bscene.domain.notification.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode implements BaseResponseCode {

    PUSH_TOKEN_SAVE_SUCCESS(OK, "NOTIFICATION200_1", "푸시 알림 토큰이 저장되었습니다."),
    PUSH_TOKEN_DELETE_SUCCESS(OK, "NOTIFICATION200_2", "푸시 알림 토큰이 삭제되었습니다.");

    private final int status;
    private final String code;
    private final String message;
}