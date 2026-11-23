package com.umc.bscene.domain.user.policy;

import java.util.regex.Pattern;

/**
 * 팬 닉네임 규칙. 온보딩·내 정보 수정·닉네임 중복 확인이 같은 규칙을 쓴다.
 * 규칙이 바뀌면 이 클래스만 고친다.
 * 판정만 하고 예외는 던지지 않는다. 에러 코드는 호출하는 쪽이 고른다.
 */
public final class FanNicknamePolicy {

    // 한글(완성형·자음·모음), 영문, 공백만 허용하고 공백 포함 1~8자
    private static final Pattern PATTERN =
            Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z ]{1,8}$");

    private FanNicknamePolicy() {
    }

    // 저장·비교에 쓰는 형태로 정리한다 (앞뒤 공백 제거)
    public static String normalize(String nickname) {
        return nickname == null ? null : nickname.strip();
    }

    // normalize 를 거친 값에 적용한다
    public static boolean isValid(String nickname) {
        return nickname != null && PATTERN.matcher(nickname).matches();
    }
}
