package com.umc.bscene.domain.band.dto;

import com.umc.bscene.domain.band.enums.BandMemberType;
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
            Long bandMemberId,
            BandMemberType memberType
    ) {
        String inviteType =
                memberType == BandMemberType.SESSION
                        ? "세션"
                        : "멤버";

        return new BandPushMessage(
                NotificationType.BAND_INVITE,
                bandName + "에서 " + inviteType + " 초대를 보냈어요",
                "밴드 " + inviteType + "로 함께 활동해 주세요.",
                "/band/notifications",
                bandMemberId
        );
    }
}
