package com.umc.bscene.domain.session.enums;

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

    public String getDescription() {
        return description;
    }
}