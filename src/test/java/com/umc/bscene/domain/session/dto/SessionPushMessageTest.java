package com.umc.bscene.domain.session.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionPushMessageTest {

    private static final Long SUBMISSION_ID = 10L;

    @Test
    void applicationSubmitted_제출된_지원서_상세로_연결한다() {
        SessionPushMessage message =
                SessionPushMessage.applicationSubmitted(
                        SUBMISSION_ID,
                        "지원자",
                        "기타 모집"
                );

        assertThat(message.deepLink())
                .isEqualTo("/band/my/applications/10");
        assertThat(message.referenceId()).isEqualTo(SUBMISSION_ID);
    }

    @Test
    void applicationDecisionForApplicant_밴드_알림으로_연결한다() {
        SessionPushMessage message =
                SessionPushMessage.applicationDecisionForApplicant(
                        SUBMISSION_ID,
                        "기타 모집",
                        true
                );

        assertThat(message.body())
                .isEqualTo("'기타 모집' 모집에서 지원을 수락했어요.");

        assertThat(message.body())
                .doesNotContain("최종 참여 여부");
        assertThat(message.deepLink())
                .isEqualTo("/band/notifications");
        assertThat(message.referenceId()).isEqualTo(SUBMISSION_ID);
    }

    @Test
    void applicationDecisionForBandMembers_밴드_알림으로_연결한다() {
        SessionPushMessage message =
                SessionPushMessage.applicationDecisionForBandMembers(
                        SUBMISSION_ID,
                        "지원자",
                        true
                );

        assertThat(message.deepLink())
                .isEqualTo("/band/notifications");
        assertThat(message.referenceId()).isEqualTo(SUBMISSION_ID);
    }

    @Test
    void applicationFinalDecisionForBandMembers_밴드_알림으로_연결한다() {
        SessionPushMessage message =
                SessionPushMessage.applicationFinalDecisionForBandMembers(
                        SUBMISSION_ID,
                        "지원자",
                        true
                );

        assertThat(message.deepLink())
                .isEqualTo("/band/notifications");
        assertThat(message.referenceId()).isEqualTo(SUBMISSION_ID);
    }

    @Test
    void deadlineReminder_공고_관리로_연결한다() {
        SessionPushMessage message =
                SessionPushMessage.deadlineReminder(
                        "기타 모집",
                        30L
                );

        assertThat(message.deepLink())
                .isEqualTo("/band/profile/postings");
        assertThat(message.referenceId()).isEqualTo(30L);
    }
}
