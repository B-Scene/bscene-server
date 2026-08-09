package com.umc.bscene.domain.follow.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CONFLICT;
import static com.umc.bscene.global.constant.StaticValue.FORBIDDEN;

@Getter
@RequiredArgsConstructor
public enum FollowErrorCode implements BaseResponseCode {

    FAN_MODE_REQUIRED(FORBIDDEN, "FOLLOW403_1", "팬 모드로 전환 후 이용할 수 있는 기능입니다."),
    OWN_BAND_FOLLOW_NOT_ALLOWED(FORBIDDEN, "FOLLOW403_2", "자신이 속한 밴드는 팔로우할 수 없습니다."),
    ALREADY_FOLLOWED(CONFLICT, "FOLLOW409_1", "이미 팔로우한 밴드입니다.");

    private final int status;
    private final String code;
    private final String message;
}
