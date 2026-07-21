package com.umc.bscene.domain.chat.dto;

import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.notification.message.PushMessage;

public record ChatPushMessage(
        NotificationType type,
        String title,
        String body,
        String deepLink,
        Long referenceId
) implements PushMessage {

    private static final int CONTENT_PREVIEW_LENGTH = 50;

    // 상대방에게 새 쪽지가 도착했음을 알리는 메시지
    public static ChatPushMessage received(
            String senderName,
            String content,
            Long chatRoomId
    ) {
        return new ChatPushMessage(
                NotificationType.MESSAGE,
                "새로운 쪽지가 도착했어요",
                senderName + ": " + createContentPreview(content),
                "/chat/rooms/" + chatRoomId,
                chatRoomId
        );
    }

    // 긴 쪽지 내용은 푸시 알림에 표시할 수 있도록 앞부분만 사용
    private static String createContentPreview(String content) {
        if (content.length() <= CONTENT_PREVIEW_LENGTH) {
            return content;
        }

        return content.substring(0, CONTENT_PREVIEW_LENGTH) + "...";
    }
}