package com.umc.bscene.domain.follow.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CREATED;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum FollowSuccessCode implements BaseResponseCode {

    FOLLOW_SUCCESS(CREATED, "FOLLOW201_1", "밴드를 팔로우했습니다."),

    UNFOLLOW_SUCCESS(OK, "FOLLOW200_1", "밴드 팔로우를 취소했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
