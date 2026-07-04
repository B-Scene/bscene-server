package com.umc.bscene.domain.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Genre {

    ROCK("록"),
    INDIE_POP("인디팝"),
    PUNK("펑크"),
    METAL("메탈"),
    JAZZ("재즈"),
    BLUES("블루스"),
    RNB("R&B"),
    ACOUSTIC("어쿠스틱"),
    FOLK("포크");

    private final String name;
}