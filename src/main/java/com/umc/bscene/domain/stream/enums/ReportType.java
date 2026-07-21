package com.umc.bscene.domain.stream.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ReportType {

    SPAM("스팸 또는 홍보성 게시물", "의도와 관계 없이 반복적인 홍보, 광고, 도배성 메시지"),
    ABUSE("욕설 및 혐오 표현", "특정 개인이나 집단에 대한 욕설, 비하, 혐오 표현"),
    SEXUAL("성적으로 불쾌한 내용", "선정적이거나 성적으로 불쾌감을 주는 표현"),
    VIOLENCE("폭력적이거나 위험한 내용", "폭력, 자해, 범죄 조장 등 위험한 행동을 유도하는 내용"),
    COPYRIGHT("저작권 침해", "무단으로 사용된 음원, 영상, 이미지 등 저작권 침해"),
    ETC("기타", "위 항목에 해당하지 않는 기타 문제"),
    ;

    private final String essence;
    private final String detail;
}
