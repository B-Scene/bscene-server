package com.umc.bscene.domain.session.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionRegionTest {

    @Test
    void acceptsEnumNameIgnoringCase() {
        assertThat(SessionRegion.fromValue("SEOUL")).isEqualTo(SessionRegion.SEOUL);
        assertThat(SessionRegion.fromValue("gyeonggi")).isEqualTo(SessionRegion.GYEONGGI);
    }

    @Test
    void rejectsDisplayNameAndUnknownRegion() {
        assertThatThrownBy(() -> SessionRegion.fromValue(SessionRegion.SEOUL.getDescription()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SessionRegion.fromValue("SEOUL_MAPO"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
