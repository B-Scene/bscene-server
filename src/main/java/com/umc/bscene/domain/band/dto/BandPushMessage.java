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

    public static BandPushMessage createRequested(String bandName, Long bandId) {
        return new BandPushMessage(
                NotificationType.BAND_VERIFY,
                "밴드 생성이 요청되었습니다",
                bandName + "이(가) 실제 있는 밴드인지 검토 후 추가될 예정이에요.",
                "/band/notifications",
                bandId
        );
    }

    public static BandPushMessage verifyAccepted(Long bandId) {
        return new BandPushMessage(
                NotificationType.BAND_VERIFY,
                "밴드 생성이 완료되었어요!",
                "이제 활동을 시작해볼까요?",
                "/band/notifications",
                bandId
        );
    }

    public static BandPushMessage verifyRejected(Long creationRequestId) {
        return new BandPushMessage(
                NotificationType.BAND_VERIFY,
                "밴드 생성이 거절되었습니다.",
                "아쉽지만 밴드 확인이 어려웠어요.",
                "/band/notifications",
                creationRequestId
        );
    }
}
