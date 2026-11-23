package com.umc.bscene.domain.user.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FanNicknamePolicyTest {

    @ParameterizedTest
    @CsvSource(value = {
            "' 밴드팬 ',밴드팬",
            "'  Rock 팬',Rock 팬",
            "밴드팬,밴드팬"
    })
    void normalize_앞뒤_공백만_잘라낸다(String nickname, String expected) {
        assertThat(FanNicknamePolicy.normalize(nickname)).isEqualTo(expected);
    }

    @Test
    void normalize_null이면_null을_반환한다() {
        assertThat(FanNicknamePolicy.normalize(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "팬", "BSCENE", "밴드 팬", "Rock 팬", "ㅋㅋ", "ㅏㅏ",
            "가나다라마바사아", "a b c d"
    })
    void isValid_한글_영문_공백_8자_이내면_통과한다(String nickname) {
        assertThat(FanNicknamePolicy.isValid(nickname)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "팬1", "fan_1", "팬!", "밴드-팬", "도도...",
            "가나다라마바사아자", ""
    })
    void isValid_숫자_특수문자_9자_이상_빈값은_거절한다(String nickname) {
        assertThat(FanNicknamePolicy.isValid(nickname)).isFalse();
    }

    @Test
    void isValid_null이면_거절한다() {
        assertThat(FanNicknamePolicy.isValid(null)).isFalse();
    }
}
