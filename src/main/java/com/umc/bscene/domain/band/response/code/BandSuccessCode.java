package com.umc.bscene.domain.band.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CREATED;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum BandSuccessCode implements BaseResponseCode {

    BAND_CREATE_SUCCESS(CREATED, "BAND201_1", "밴드가 생성되었습니다."),
    BAND_MEMBER_INVITE_SUCCESS(CREATED, "BAND201_2", "밴드 멤버를 초대했습니다."),

    BAND_MEMBER_ACCEPT_SUCCESS(OK, "BAND200_1", "밴드 초대를 수락했습니다."),
    BAND_MEMBER_REMOVE_SUCCESS(OK, "BAND200_2", "밴드 멤버를 제거했습니다."),
    BAND_MEMBER_LIST_GET_SUCCESS(OK, "BAND200_3", "밴드 멤버 목록을 조회했습니다."),
    BAND_MEMBER_REJECT_SUCCESS(OK, "BAND200_4", "밴드 초대를 거절했습니다."),
    BAND_MEMBER_SEARCH_SUCCESS(OK, "BAND200_5", "검색 결과를 조회했습니다."),
    BAND_NAME_AVAILABLE(OK, "BAND200_6", "사용 가능한 밴드명이에요."),
    BAND_NAME_DUPLICATED(OK, "BAND200_7", "이미 사용 중인 밴드명이에요."),
    MUSIC_LINK_GET_SUCCESS(OK, "MUSIC200_1", "음원 링크를 조회했습니다."),
    MUSIC_LINK_SAVE_SUCCESS(OK, "MUSIC200_2", "저장됐어요.");

    private final int status;
    private final String code;
    private final String message;
}
