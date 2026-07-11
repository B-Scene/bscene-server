package com.umc.bscene.domain.fanhome.port;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.BandNewsItem;

import java.util.List;

/**
 * 팬홈이 팔로우한 밴드들의 소식을 조회하기 위한 포트 (adapter는 post 도메인이 구현).
 */
public interface PostPort {
    /**
     * 주어진 밴드들의 최근 소식을 최신순으로 조회합니다.
     *
     * @param bandIds 소식을 조회할 밴드 ID 목록
     * @param limit   최대 조회 개수
     * @return 최신순으로 정렬된 밴드 소식 목록
     */
    List<BandNewsItem> findRecentNews(List<Long> bandIds, int limit);
}
