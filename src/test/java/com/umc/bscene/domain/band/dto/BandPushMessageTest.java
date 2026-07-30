package com.umc.bscene.domain.band.dto;

import com.umc.bscene.domain.band.enums.BandMemberType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BandPushMessageTest {

    @Test
    void memberInvited_멤버와_세션_초대는_밴드_알림으로_연결한다() {
        BandPushMessage memberMessage = BandPushMessage.memberInvited(
                "테스트 밴드",
                10L,
                BandMemberType.MEMBER
        );
        BandPushMessage sessionMessage = BandPushMessage.memberInvited(
                "테스트 밴드",
                20L,
                BandMemberType.SESSION
        );

        assertThat(memberMessage.deepLink())
                .isEqualTo("/band/notifications");
        assertThat(sessionMessage.deepLink())
                .isEqualTo("/band/notifications");
        assertThat(memberMessage.referenceId()).isEqualTo(10L);
        assertThat(sessionMessage.referenceId()).isEqualTo(20L);
    }
}
