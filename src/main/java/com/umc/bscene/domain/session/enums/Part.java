package com.umc.bscene.domain.session.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Part {

    VOCAL("보컬"),
    GUITAR("기타"),
    BASS("베이스"),
    KEYBOARD("키보드"),
    DRUM("드럼");

    private final String description;

    Part(String description) {
        this.description = description;
    }

    // JSON 응답 시 enum 이름(VOCAL)이 아닌 한글(보컬)로 반환
    @JsonValue
    public String getDescription() {
        return description;
    }
}