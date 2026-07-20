package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;

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

    /**
     * 사용자가 팔로우한 밴드 목록을 조회합니다. (밴드명 가나다순, offset 무한스크롤)
     *
     * @param userId 조회할 사용자 ID
     * @param page   0-base 페이지 번호
     * @param size   페이지 크기
     * @return 팔로우한 밴드 목록 (totalCount는 첫 페이지에서만 채워짐)
     */
    FollowedBandResponse findFollowedBands(Long userId, int page, int size);
}
