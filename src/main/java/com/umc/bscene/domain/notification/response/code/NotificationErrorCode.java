package com.umc.bscene.domain.notification.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.BAD_GATEWAY;
import static com.umc.bscene.global.constant.StaticValue.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseResponseCode {

    RECEIVER_NOT_FOUND(NOT_FOUND, "NOTIFICATION404_1", "알림을 받을 사용자를 찾을 수 없습니다."),
    NOTIFICATION_NOT_FOUND(NOT_FOUND, "NOTIFICATION404_2", "존재하지 않는 알림입니다."),
    FCM_SEND_FAILED(BAD_GATEWAY, "NOTIFICATION502_1", "푸시 알림 발송에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;
}