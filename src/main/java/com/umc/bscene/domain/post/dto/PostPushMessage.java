package com.umc.bscene.domain.post.dto;

import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.notification.message.PushMessage;

public record PostPushMessage(
        NotificationType type,
        String title,
        String body,
        String deepLink,
        Long referenceId
) implements PushMessage {

    // 팔로우한 밴드의 게시물 등록 알림
    public static PostPushMessage created(
            String bandName,
            String postTitle,
            Long postId
    ) {
        return new PostPushMessage(
                NotificationType.POST,
                bandName + "의 새로운 게시물이 등록됐어요",
                "'" + postTitle + "' 게시물을 확인해보세요.",
                "/fan/explore/contents/" + postId,
                postId
        );
    }
}
