package com.umc.bscene.domain.session.enums;

public enum SkillLevel {

    BEGINNER("입문"),
    INTERMEDIATE("중급"),
    ADVANCED("상급");

    private final String description;

    SkillLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}