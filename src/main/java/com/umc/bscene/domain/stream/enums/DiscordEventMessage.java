package com.umc.bscene.domain.stream.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum DiscordEventMessage {

    USER_REPORT_EVENT(
            """
                    ## 🚨 라이브 채팅 내 신고
                    - 신고자 기본 키 : %d
                    - 신고 대상 기본 키 : %d
                    - 신고 유형 : %s
                    - 채팅 내역 : %s
                    - 상세 설명 : %s
                    """
    );

    private final String content;
}
