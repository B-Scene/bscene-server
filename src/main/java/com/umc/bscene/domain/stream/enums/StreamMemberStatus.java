package com.umc.bscene.domain.stream.enums;

public enum StreamMemberStatus {

    // 멤버 초대 여부. 오디오 송출자는 이 상태와 관계 없이 바로 ACCEPTED로 넣어 빌드
    INVITED,
    ACCEPTED,
    REJECTED,

    // 아래 값들은 예약 편집 화면 응답 표시 전용. DB(StreamMember.status)에는 저장하지 말 것
    OWNER,      // 송출자(라이브 생성자) 표시용
    APPROVED,   // ACCEPTED의 응답 표기
}
