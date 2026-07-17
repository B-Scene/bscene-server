package com.umc.bscene.domain.search.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum SearchSuccessCode implements BaseResponseCode {

    SEARCH_SUCCESS(OK, "SEARCH200_1", "검색 결과를 조회했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
