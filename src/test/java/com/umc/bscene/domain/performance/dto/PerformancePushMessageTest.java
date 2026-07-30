package com.umc.bscene.domain.performance.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PerformancePushMessageTest {

    private static final Long PERFORMANCE_ID = 10L;

    @Test
    void 모든_공연_알림은_팬_공연_상세로_연결한다() {
        PerformancePushMessage created =
                PerformancePushMessage.created(
                        "테스트 밴드",
                        "공연 제목",
                        PERFORMANCE_ID
                );
        PerformancePushMessage reminder =
                PerformancePushMessage.reminder(
                        "테스트 밴드",
                        "공연 제목",
                        PERFORMANCE_ID
                );
        PerformancePushMessage updated =
                PerformancePushMessage.updated(
                        "테스트 밴드",
                        "공연 제목",
                        PERFORMANCE_ID
                );

        assertThat(created.deepLink())
                .isEqualTo("/fan/home/concerts/10");
        assertThat(reminder.deepLink())
                .isEqualTo("/fan/home/concerts/10");
        assertThat(updated.deepLink())
                .isEqualTo("/fan/home/concerts/10");
        assertThat(created.referenceId()).isEqualTo(PERFORMANCE_ID);
        assertThat(reminder.referenceId()).isEqualTo(PERFORMANCE_ID);
        assertThat(updated.referenceId()).isEqualTo(PERFORMANCE_ID);
    }
}
