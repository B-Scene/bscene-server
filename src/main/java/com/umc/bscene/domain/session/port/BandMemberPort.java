package com.umc.bscene.domain.session.port;

import java.util.List;

public interface BandMemberPort {

    // 밴드에 소속된 구성원의 사용자 ID를 조회
    List<Long> getAcceptedMemberUserIds(Long bandId);
}