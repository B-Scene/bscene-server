package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;

import java.util.Map;

/**
 * 마이페이지가 팔로우 정보를 조회하기 위한 포트 (adapter는 follow 도메인이 구현).
 */
public interface FollowPort {

    /**
     * 사용자가 팔로우한 밴드 수를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 팔로우한 밴드 수
     */
    long countFollowing(Long userId);

    /**
     * 사용자가 팔로우한 밴드들의 장르별 밴드 수를 조회합니다. (마이페이지 대표 장르 산출용)
     *
     * @param userId 조회할 사용자 ID
     * @return 장르 → 팔로우한 밴드 수 (팔로우가 없으면 빈 Map)
     */
    Map<Genre, Long> countFollowedBandsGroupedByGenre(Long userId);
}
