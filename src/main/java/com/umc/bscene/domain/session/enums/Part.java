package com.umc.bscene.domain.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Part {

    VOCAL("보컬"),
    GUITAR("기타"),
    BASS("베이스"),
    KEYBOARD("키보드"),
    DRUM("드럼"),
    ETC("etc");

    private final String description;

    Part(String description) {
        this.description = description;
    }

    @JsonCreator
    public static Part fromValue(String value) {
        for (Part part : values()) {
            if (part.name().equalsIgnoreCase(value) || part.description.equals(value)) {
                return part;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 파트입니다: " + value);
    }

    @JsonValue
    public String getDescription() {
        return description;
    }
}
