package com.umc.bscene.domain.session.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionRegionTest {

    @Test
    void acceptsEnumNameAndDisplayName() {
        assertThat(SessionRegion.fromValue("SEOUL")).isEqualTo(SessionRegion.SEOUL);
        assertThat(SessionRegion.fromValue("gyeonggi")).isEqualTo(SessionRegion.GYEONGGI);
        assertThat(SessionRegion.fromValue(SessionRegion.SEOUL.getDescription()))
                .isEqualTo(SessionRegion.SEOUL);
    }

    @Test
    void rejectsUnknownRegion() {
        assertThatThrownBy(() -> SessionRegion.fromValue("SEOUL_MAPO"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
