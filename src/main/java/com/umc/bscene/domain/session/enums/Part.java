package com.umc.bscene.domain.session.enums;


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

    public String getDescription() {
        return description;
    }
}