package com.umc.bscene.domain.band.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum BandErrorCode implements BaseResponseCode {

    NOT_BAND_OWNER(FORBIDDEN, "BAND403_1", "밴드 오너만 수행할 수 있는 작업입니다."),
    CANNOT_REMOVE_OWNER(FORBIDDEN, "BAND403_2", "밴드 오너는 제거할 수 없습니다."),

    BAND_NOT_FOUND(NOT_FOUND, "BAND404_1", "존재하지 않는 밴드입니다."),
    BAND_MEMBER_NOT_FOUND(NOT_FOUND, "BAND404_2", "존재하지 않는 밴드 멤버입니다."),
    INVITEE_NOT_FOUND(NOT_FOUND, "BAND404_3", "초대할 사용자를 찾을 수 없습니다."),

    ALREADY_BAND_MEMBER(CONFLICT, "BAND409_1", "이미 밴드에 초대되었거나 소속된 사용자입니다."),
    DUPLICATE_BAND_NAME(CONFLICT, "BAND409_2", "이미 사용 중인 밴드명이에요."),
    ALREADY_ACCEPTED_MEMBER(CONFLICT, "BAND409_3", "이미 수락된 멤버는 거절할 수 없습니다."),
    ALREADY_ACCEPTED_INVITE(CONFLICT, "BAND409_4", "이미 수락된 초대입니다.");

    private final int status;
    private final String code;
    private final String message;
}
