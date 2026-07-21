package com.umc.bscene.domain.stream.port;

import java.util.List;

public interface FollowPort {

    /**
     * 팔로우한 밴드의 라이브 목록 조회에서 사용자가 팔로우한 밴드 ID 목록을 전송받기 위한 메소드입니다.
     * @param userId 조회할 사용자의 ID를 전달합니다.
     * @return 사용자가 팔로우한 밴드 ID의 List를 반환해주세요.
     */
    List<Long> getFollowingBandIds(Long userId);

    /**
     * 라이브 예약/시작 푸시 알림의 발송 대상을 구하기 위해, 특정 밴드를 팔로우한 사용자 ID 목록을 전송받기 위한 메소드입니다.
     * @param bandId 팔로워를 조회할 밴드의 ID를 전달합니다.
     * @return 해당 밴드를 팔로우한 사용자 ID의 List를 반환해주세요.
     */
    List<Long> getFollowerUserIdsByBandId(Long bandId);
}
