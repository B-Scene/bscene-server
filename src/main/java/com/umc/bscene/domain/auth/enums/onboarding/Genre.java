package com.umc.bscene.domain.auth.enums.onboarding;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Genre {

    METAL("메탈"),
    BLUES("블루스"),
    PSYCHEDELIC_ROCK("사이키델릭록"),
    ALTERNATIVE_ROCK("얼터너티브록"),
    INDIE("인디"),
    ELECTRONIC_ROCK("일렉트로닉록"),
    JAZZ("재즈"),
    POP("팝"),
    POP_ROCK("팝록"),
    PUNK_ROCK("펑크록"),
    FOLK_ROCK("포크록"),
    HARD_ROCK("하드록"),
    ETC("기타");

    private final String name;
}
