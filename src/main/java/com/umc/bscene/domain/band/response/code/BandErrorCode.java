package com.umc.bscene.domain.band.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum BandErrorCode implements BaseResponseCode {

    INVALID_ETC_MUSIC_LINK(BAD_REQUEST, "MUSIC400_1", "플랫폼과 링크를 함께 입력해주세요."),
    NOT_OWN_BAND_MEMBER_PROFILE(BAD_REQUEST, "PROFILE4001", "본인의 멤버 프로필만 선택할 수 있어요."),

    NOT_BAND_OWNER(FORBIDDEN, "BAND403_1", "밴드 오너만 수행할 수 있는 작업입니다."),
    CANNOT_REMOVE_OWNER(FORBIDDEN, "BAND403_2", "밴드 오너는 제거할 수 없습니다."),
    BAND_PERMISSION_DENIED(FORBIDDEN, "BAND403_3", "밴드 멤버만 사용할 수 있는 기능입니다."),
    NOT_BAND_MEMBER(FORBIDDEN, "MUSIC403_1", "음원 링크를 수정할 권한이 없어요."),
    BAND_MEMBER_INVITE_FORBIDDEN(FORBIDDEN, "MEMBER4032", "멤버 초대는 밴드 리더만 할 수 있어요."),
    NOT_INVITED_MEMBER(FORBIDDEN, "MEMBER4031", "초대받은 본인만 응답할 수 있어요."),

    BAND_NOT_FOUND(NOT_FOUND, "BAND404_1", "존재하지 않는 밴드입니다."),
    BAND_MEMBER_NOT_FOUND(NOT_FOUND, "BAND404_2", "존재하지 않는 밴드 멤버입니다."),
    INVITEE_NOT_FOUND(NOT_FOUND, "BAND404_3", "초대할 사용자를 찾을 수 없습니다."),
    BAND_MEMBER_PROFILE_NOT_FOUND(NOT_FOUND, "BAND404_4", "존재하지 않는 멤버 프로필입니다."),

    ALREADY_BAND_MEMBER(CONFLICT, "MEMBER4091", "이미 초대했거나 멤버인 유저예요."),
    DUPLICATE_BAND_NAME(CONFLICT, "BAND409_2", "이미 사용 중인 밴드명이에요."),
    ALREADY_ACCEPTED_MEMBER(CONFLICT, "BAND409_3", "이미 수락된 멤버는 거절할 수 없습니다."),
    INVITE_ALREADY_PROCESSED(CONFLICT, "MEMBER4092", "이미 처리된 초대예요.");

    private final int status;
    private final String code;
    private final String message;
}
