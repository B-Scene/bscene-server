package com.umc.bscene.domain.session.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SkillLevel {

    BEGINNER("입문"),
    INTERMEDIATE("중급"),
    ADVANCED("상급");

    private final String description;

    SkillLevel(String description) {
        this.description = description;
    }

    // JSON 응답 시 enum 이름(BEGINNER)이 아닌 한글(입문)로 반환
    @JsonValue
    public String getDescription() {
        return description;
    }
}