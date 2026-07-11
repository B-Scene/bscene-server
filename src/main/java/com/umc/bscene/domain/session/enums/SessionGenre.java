package com.umc.bscene.domain.session.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum SessionGenre {

    ROCK("록"),
    INDIE_POP("인디팝"),
    JAZZ("재즈"),
    METAL("메탈"),
    FOLK("포크"),
    RNB("R&B"),
    BLUES("블루스"),
    PUNK("펑크"),
    ACOUSTIC("어쿠스틱");

    private final String description;

    SessionGenre(String description) {
        this.description = description;
    }

    @JsonCreator
    public static SessionGenre fromValue(String value) {
        for (SessionGenre genre : values()) {
            if (genre.name().equalsIgnoreCase(value) || genre.description.equals(value)) {
                return genre;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 장르입니다: " + value);
    }

    // JSON 응답 시 enum 이름(ROCK)이 아닌 한글(록)로 반환
    @JsonValue
    public String getDescription() {
        return description;
    }
}
