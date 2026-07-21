package com.umc.bscene.domain.session.converter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionEnumDeserializerTest {

    @Test
    void genreAcceptsKoreanAndEnglish() {
        assertThat(SessionGenreJsonDeserializer.fromKorean("인디")).isEqualTo(Genre.INDIE);
        assertThat(SessionGenreJsonDeserializer.fromKorean("INDIE")).isEqualTo(Genre.INDIE);
    }

    @Test
    void regionAcceptsKoreanAndEnglish() {
        assertThat(SessionRegionJsonDeserializer.fromKorean("서울")).isEqualTo(Region.SEOUL);
        assertThat(SessionRegionJsonDeserializer.fromKorean("SEOUL")).isEqualTo(Region.SEOUL);
    }

    @Test
    void englishCodesMustBeUppercase() {
        assertThatThrownBy(() -> SessionGenreJsonDeserializer.fromKorean("indie"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SessionRegionJsonDeserializer.fromKorean("seoul"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
