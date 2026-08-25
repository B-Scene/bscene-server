package com.umc.bscene.domain.band.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum BandErrorCode implements BaseResponseCode {

    INVALID_ETC_MUSIC_LINK(BAD_REQUEST, "MUSIC400_1", "플랫폼과 링크를 함께 입력해주세요."),
    NOT_OWN_BAND_MEMBER_PROFILE(BAD_REQUEST, "PROFILE400_1", "본인의 멤버 프로필만 선택할 수 있어요."),
    INVALID_MEMBER_SEARCH_KEYWORD(BAD_REQUEST, "MEMBER400_1", "검색어를 한 글자 이상 입력해주세요."),
    INVALID_OWNER_TRANSFER_TARGET(BAD_REQUEST, "BAND400_2", "오너 권한은 밴드원 내 MEMBER에게만 양도할 수 있습니다."),
    CANNOT_TRANSFER_OWNER_TO_SELF(BAD_REQUEST, "BAND400_3", "현재 오너에게는 권한을 양도할 수 없습니다."),
    INVALID_PROFILE_IMAGE_UPDATE(BAD_REQUEST, "BAND400_4", "프로필 이미지 변경과 삭제를 동시에 요청할 수 없습니다."),

    BAND_INVITE_LINK_EXPIRED(BAD_REQUEST, "BAND400_1", "만료된 밴드 초대 링크입니다."),
    NOT_BAND_OWNER(FORBIDDEN, "BAND403_1", "밴드 오너만 수행할 수 있는 작업입니다."),
    CANNOT_REMOVE_OWNER(FORBIDDEN, "BAND403_2", "밴드 오너는 제거할 수 없습니다."),
    BAND_PERMISSION_DENIED(FORBIDDEN, "BAND403_3", "밴드 멤버만 사용할 수 있는 기능입니다."),
    NOT_BAND_MEMBER(FORBIDDEN, "MUSIC403_1", "음원 링크를 수정할 권한이 없어요."),
    BAND_MEMBER_INVITE_FORBIDDEN(FORBIDDEN, "MEMBER403_2", "멤버 초대는 밴드 리더만 할 수 있어요."),
    NOT_INVITED_MEMBER(FORBIDDEN, "MEMBER403_1", "초대받은 본인만 응답할 수 있어요."),
    PROFILE_ACTIVATION_FORBIDDEN(FORBIDDEN, "MEMBER403_2", "프로필 소유자만 활성화 여부를 업데이트할 수 있습니다."),
    // 멤버는 맞지만 해당 밴드 프로필로 전환되지 않은 상태 - FE가 모드 전환 후 재시도할 수 있는 복구 가능 403
    BAND_MODE_REQUIRED(FORBIDDEN, "BAND403_4", "해당 밴드 모드로 전환 후 이용할 수 있는 기능입니다."),
    BAND_OWNER_CANNOT_LEAVE(FORBIDDEN, "BAND403_5", "밴드 오너는 밴드를 탈퇴할 수 없습니다."),
    // 검수(PENDING) 중에는 게시물·공연·세션모집 등 활동 생성 불가 - 활동 FK가 생기면 검수 거절(밴드 삭제)이 불가능해짐
    BAND_NOT_VERIFIED(FORBIDDEN, "BAND403_6", "검수 중인 밴드는 아직 활동할 수 없어요."),


    BAND_NOT_FOUND(NOT_FOUND, "BAND404_1", "존재하지 않는 밴드입니다."),
    BAND_MEMBER_NOT_FOUND(NOT_FOUND, "BAND404_2", "존재하지 않는 밴드 멤버입니다."),
    INVITEE_NOT_FOUND(NOT_FOUND, "BAND404_3", "초대할 사용자를 찾을 수 없습니다."),
    BAND_MEMBER_PROFILE_NOT_FOUND(NOT_FOUND, "BAND404_4", "존재하지 않는 멤버 프로필입니다."),
    BAND_INVITE_LINK_NOT_FOUND(NOT_FOUND, "BAND404_5", "존재하지 않는 밴드 초대 링크입니다."),

    ALREADY_BAND_MEMBER(CONFLICT, "MEMBER409_1", "이미 초대했거나 멤버인 유저예요."),
    DUPLICATE_BAND_NAME(CONFLICT, "BAND409_2", "이미 사용 중인 밴드명이에요."),
    ALREADY_ACCEPTED_MEMBER(CONFLICT, "BAND409_3", "이미 수락된 멤버는 거절할 수 없습니다."),
    INVITE_ALREADY_PROCESSED(CONFLICT, "MEMBER409_2", "이미 처리된 초대예요."),
    BAND_MEMBER_PROFILE_IN_USE(CONFLICT, "PROFILE409_1", "밴드에서 사용 중인 멤버 프로필은 삭제할 수 없어요."),
    // audio_stream.band_id는 FK가 아니라 검수 삭제 안전장치(FK 제약 롤백)에 걸리지 않으므로 애플리케이션 단에서 차단
    BAND_HAS_LIVE_HISTORY(CONFLICT, "BAND409_4", "라이브 이력이 있는 밴드는 삭제할 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
