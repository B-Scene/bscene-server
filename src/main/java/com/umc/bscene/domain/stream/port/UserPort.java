package com.umc.bscene.domain.stream.port;

import com.umc.bscene.domain.user.entity.User;

import java.util.Collection;
import java.util.List;

public interface UserPort {

    /**
     * 공동 진행자로 새로 추가할 사용자들의 정보를 ID 목록으로 조회하는 메소드입니다.
     * 전달한 ID 중 존재하지 않는 사용자가 있으면 해당 사용자는 결과에 포함되지 않으며,
     * 호출부에서 요청한 ID 개수와 조회 결과 개수를 비교하여 사용자 존재 여부를 검증합니다.
     *
     * @param userIds 조회할 사용자 ID 목록을 전달합니다.
     * @return 존재하는 사용자 엔티티 목록을 반환해주세요.
     */
    List<User> findAllByIds(Collection<Long> userIds);

    boolean validAccessAboutStreamInBandMode(Long userId, Collection<Long> coHostIds);

    // 라이브 방 입장 닉네임 분기용: 현재 밴드 모드인지 여부 (모드 미정이면 false)
    boolean isBandMode(Long userId);

    String getFanName(Long userId);
}
