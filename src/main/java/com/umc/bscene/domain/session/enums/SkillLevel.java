package com.umc.bscene.domain.session.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum SkillLevel {

    BEGINNER("입문"),
    INTERMEDIATE("중급"),
    ADVANCED("상급");

    private final String description;

    SkillLevel(String description) {
        this.description = description;
    }

    @JsonCreator
    public static SkillLevel fromValue(String value) {
        for (SkillLevel skillLevel : values()) {
            if (skillLevel.name().equalsIgnoreCase(value) || skillLevel.description.equals(value)) {
                return skillLevel;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 실력대입니다: " + value);
    }

    // JSON 응답 시 enum 이름(BEGINNER)이 아닌 한글(입문)로 반환
    @JsonValue
    public String getDescription() {
        return description;
    }
}
