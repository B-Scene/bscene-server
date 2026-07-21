package com.umc.bscene.domain.search.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum SearchSuccessCode implements BaseResponseCode {

    SEARCH_SUCCESS(OK, "SEARCH200_1", "검색 결과를 조회했습니다."),

    RECENT_SEARCH_LIST_SUCCESS(OK, "SEARCH200_2", "최근 검색어를 조회했습니다."),
    RECENT_SEARCH_DELETE_SUCCESS(OK, "SEARCH200_3", "최근 검색어를 삭제했습니다."),
    RECENT_SEARCH_DELETE_ALL_SUCCESS(OK, "SEARCH200_4", "최근 검색어를 전체 삭제했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
