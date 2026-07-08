package com.umc.bscene.domain.follow.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CONFLICT;

@Getter
@RequiredArgsConstructor
public enum FollowErrorCode implements BaseResponseCode {

    ALREADY_FOLLOWED(CONFLICT, "FOLLOW409_1", "이미 팔로우한 밴드입니다.");

    private final int status;
    private final String code;
    private final String message;
}
