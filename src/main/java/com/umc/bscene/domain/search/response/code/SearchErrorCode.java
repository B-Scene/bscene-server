package com.umc.bscene.domain.search.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.BAD_REQUEST;
import static com.umc.bscene.global.constant.StaticValue.SERVICE_UNAVAILABLE;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements BaseResponseCode {

    KEYWORD_REQUIRED(BAD_REQUEST, "SEARCH400_1", "검색어를 입력해주세요."),

    INVALID_CURSOR(BAD_REQUEST, "SEARCH400_2", "유효하지 않은 커서예요."),

    // ES 장애 시에도 서비스 전체가 아닌 검색만 실패하도록 503으로 격리
    SEARCH_UNAVAILABLE(SERVICE_UNAVAILABLE, "SEARCH503_1", "검색 서비스를 일시적으로 사용할 수 없어요. 잠시 후 다시 시도해주세요.");

    private final int status;
    private final String code;
    private final String message;
}
