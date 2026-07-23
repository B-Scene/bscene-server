package com.umc.bscene.domain.band.dto;

import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.notification.message.PushMessage;

public record BandPushMessage(
        NotificationType type,
        String title,
        String body,
        String deepLink,
        Long referenceId
) implements PushMessage {

    public static BandPushMessage memberInvited(
            String bandName,
            Long bandMemberId
    ) {
        return new BandPushMessage(
                NotificationType.BAND_INVITE,
                bandName + "에서 멤버 초대를 보냈어요",
                "밴드 멤버로 함께 활동해 주세요.",
                "/notifications?focusInviteId=" + bandMemberId,
                bandMemberId
        );
    }
}