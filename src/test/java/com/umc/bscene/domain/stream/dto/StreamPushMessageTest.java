package com.umc.bscene.domain.stream.dto;

import com.umc.bscene.global.notification.enums.NotificationSettingType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamPushMessageTest {

    private static final Long LIVE_ID = 10L;

    @Test
    void scheduled_팬과_밴드의_예약_라이브_경로를_구분한다() {
        StreamPushMessage fanMessage = StreamPushMessage.scheduled(
                NotificationSettingType.FAN_SCHEDULED_LIVE_REMINDER,
                "테스트 밴드",
                "라이브 제목",
                "7.31. 오후 8:00",
                LIVE_ID
        );
        StreamPushMessage bandMessage = StreamPushMessage.scheduled(
                NotificationSettingType.BAND_SCHEDULED_LIVE_REMINDER,
                "테스트 밴드",
                "라이브 제목",
                "7.31. 오후 8:00",
                LIVE_ID
        );

        assertThat(fanMessage.deepLink())
                .isEqualTo("/fan/live/scheduled");
        assertThat(bandMessage.deepLink()).isEqualTo("/band/live");
    }

    @Test
    void started_팬과_밴드의_라이브_시작_경로를_구분한다() {
        StreamPushMessage fanMessage = StreamPushMessage.started(
                NotificationSettingType.FAN_FOLLOWED_BAND_LIVE_START,
                "테스트 밴드",
                "라이브 제목",
                LIVE_ID
        );
        StreamPushMessage bandMessage = StreamPushMessage.started(
                NotificationSettingType.BAND_LIVE_START_STATUS,
                "테스트 밴드",
                "라이브 제목",
                LIVE_ID
        );

        assertThat(fanMessage.deepLink())
                .isEqualTo("/fan/live/room/10");
        assertThat(bandMessage.deepLink()).isEqualTo("/band/live");
    }

    @Test
    void reminder_팬과_밴드의_라이브_리마인드_경로를_구분한다() {
        StreamPushMessage fanMessage = StreamPushMessage.reminder(
                NotificationSettingType.FAN_SCHEDULED_LIVE_REMINDER,
                "테스트 밴드",
                "라이브 제목",
                LIVE_ID
        );
        StreamPushMessage bandMessage = StreamPushMessage.reminder(
                NotificationSettingType.BAND_SCHEDULED_LIVE_REMINDER,
                "테스트 밴드",
                "라이브 제목",
                LIVE_ID
        );

        assertThat(fanMessage.deepLink())
                .isEqualTo("/fan/live/scheduled");
        assertThat(bandMessage.deepLink()).isEqualTo("/band/live");
    }

    @Test
    void replayReady_팬_다시보기_상세로_연결한다() {
        StreamPushMessage message = StreamPushMessage.replayReady(
                "테스트 밴드",
                "라이브 제목",
                LIVE_ID
        );

        assertThat(message.deepLink())
                .isEqualTo("/fan/live/replays/10");
        assertThat(message.referenceId()).isEqualTo(LIVE_ID);
    }

    @Test
    void 공동_진행자_초대와_결과는_밴드_알림으로_연결한다() {
        StreamPushMessage invitation = StreamPushMessage.coHostInvited(
                "테스트 밴드",
                "라이브 제목",
                LIVE_ID
        );
        StreamPushMessage decision =
                StreamPushMessage.coHostInvitationDecided(
                        "공동 진행자",
                        "라이브 제목",
                        true,
                        LIVE_ID
                );

        assertThat(invitation.deepLink())
                .isEqualTo("/band/notifications");
        assertThat(decision.deepLink())
                .isEqualTo("/band/notifications");
        assertThat(invitation.referenceId()).isEqualTo(LIVE_ID);
        assertThat(decision.referenceId()).isEqualTo(LIVE_ID);
    }
}
