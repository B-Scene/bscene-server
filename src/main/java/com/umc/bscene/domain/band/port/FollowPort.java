package com.umc.bscene.domain.band.port;

import com.umc.bscene.domain.band.dto.FollowerBlock;
import com.umc.bscene.global.response.CursorPage;

import java.util.List;

public interface FollowPort {
    /**
     * 밴드의 팔로워 수를 조회합니다.
     *
     * @param bandId 팔로워 수를 조회할 밴드 ID
     * @return 해당 밴드를 팔로우한 사용자 수
     */
    Long countFollowersByBandId(Long bandId);

    /**
     * 팔로워를 커서 페이징하기 위해 조회하는 쿼리
     * @param bandId 연관된 팔로워를 조회할 수 있게 bandId를 전달
     * @param cursor 커서 페이징을 위한 파라미터
     * @param size 커서 페이징을 위한 파라미터
     * @return 팬의 userId, 팬 프로필 이미지, 팬 닉네임을 리스트로 반환합니다.
     */
    CursorPage<FollowerBlock> findPagedMyBandFollowers(Long bandId, Long cursor, Integer size);

    /**
     * 사용자가 밴드를 팔로우 중인지 확인합니다. (팬모드 밴드 상세의 팔로우 버튼 상태용)
     *
     * @param userId 확인할 사용자 ID
     * @param bandId 확인할 밴드 ID
     * @return 팔로우 중이면 true
     */
    boolean isFollowing(Long userId, Long bandId);
}