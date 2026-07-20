package com.umc.bscene.domain.notification.dto.response;

import com.umc.bscene.domain.notification.enums.PushSendStatus;

public record PushSendResult(
        PushSendStatus status,
        String errorCode,
        String errorMessage
) {

    public static PushSendResult success() {
        return new PushSendResult(
                PushSendStatus.SUCCESS,
                null,
                null
        );
    }

    public static PushSendResult invalidToken(
            String errorCode,
            String errorMessage
    ) {
        return new PushSendResult(
                PushSendStatus.INVALID_TOKEN,
                errorCode,
                errorMessage
        );
    }

    public static PushSendResult failed(
            String errorCode,
            String errorMessage
    ) {
        return new PushSendResult(
                PushSendStatus.FAILED,
                errorCode,
                errorMessage
        );
    }

    public static PushSendResult skipped() {
        return new PushSendResult(
                PushSendStatus.SKIPPED,
                null,
                null
        );
    }

    public boolean isInvalidToken() {
        return status == PushSendStatus.INVALID_TOKEN;
    }

    public boolean isSuccess() {
        return status == PushSendStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == PushSendStatus.FAILED;
    }
}