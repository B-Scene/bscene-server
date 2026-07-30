package com.umc.bscene.domain.post.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostPushMessageTest {

    @Test
    void created_팬_게시물_상세로_연결한다() {
        PostPushMessage message = PostPushMessage.created(
                "테스트 밴드",
                "게시물 제목",
                10L
        );

        assertThat(message.deepLink())
                .isEqualTo("/fan/explore/contents/10");
        assertThat(message.referenceId()).isEqualTo(10L);
    }
}
