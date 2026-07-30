package com.umc.bscene.domain.chat.dto;

import com.umc.bscene.global.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPushMessageTest {

    @Test
    void received_쪽지방의_실제_프론트_경로와_참조값을_생성한다() {
        ChatPushMessage message = ChatPushMessage.received(
                "보낸사람",
                "안녕하세요",
                15L
        );

        assertThat(message.type()).isEqualTo(NotificationType.MESSAGE);
        assertThat(message.title()).isEqualTo("새로운 쪽지가 도착했어요");
        assertThat(message.body()).isEqualTo("보낸사람: 안녕하세요");
        assertThat(message.deepLink())
                .isEqualTo("/band/session/messages/15");
        assertThat(message.referenceId()).isEqualTo(15L);
    }

    @Test
    void received_50자_이하의_쪽지는_자르지_않는다() {
        String content = "가".repeat(50);

        ChatPushMessage message = ChatPushMessage.received(
                "보낸사람",
                content,
                15L
        );

        assertThat(message.body())
                .isEqualTo("보낸사람: " + content);
    }

    @Test
    void received_50자를_초과한_쪽지는_앞의_50자만_보여준다() {
        String content = "가".repeat(51);

        ChatPushMessage message = ChatPushMessage.received(
                "보낸사람",
                content,
                15L
        );

        assertThat(message.body())
                .isEqualTo("보낸사람: " + "가".repeat(50) + "...");
    }
}
