package com.umc.bscene.domain.stream.dto;

import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.notification.message.PushMessage;

// 라이브 예약/시작 푸시 발송 요청에 사용하는 PushMessage 구현체
public record StreamPushMessage(
        NotificationType type,
        String title,
        String body,
        String deepLink,
        Long referenceId
) implements PushMessage {

    // FIXME: 와이어프레임 확정 후 title/body 문구, deepLink 포맷 채우기

    // 라이브 예약 생성 알림
    public static StreamPushMessage scheduled(String bandName, String liveTitle, String scheduledAtText, Long liveId) {
        return new StreamPushMessage(
                NotificationType.LIVE,
                bandName + " 라이브가 예약됐어요",
                scheduledAtText + "에 '" + liveTitle + "' 라이브가 시작될 예정이에요.",
                "/lives/" + liveId,
                liveId
        );
    }

    // 라이브 시작 알림
    public static StreamPushMessage started(String bandName, String liveTitle, Long liveId) {
        return new StreamPushMessage(
                NotificationType.LIVE,
                bandName + " 라이브가 시작됐어요",
                "'" + liveTitle + "' 라이브에 지금 참여해보세요.",
                "/lives/" + liveId,
                liveId
        );
    }

    // 예정 라이브 시작 30분 전 리마인드 알림
    public static StreamPushMessage reminder(String bandName, String liveTitle, Long liveId) {
        return new StreamPushMessage(
                NotificationType.LIVE,
                bandName + " 라이브가 30분 후 시작돼요",
                "'" + liveTitle + "' 라이브가 곧 시작될 예정이에요.",
                "/lives/" + liveId,
                liveId
        );
    }
}
