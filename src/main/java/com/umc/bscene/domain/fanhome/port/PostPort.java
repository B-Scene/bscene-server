package com.umc.bscene.domain.fanhome.port;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.BandNewsItem;
import com.umc.bscene.domain.fanhome.dto.response.FollowingBandNewsResponse;

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

    /**
     * 주어진 밴드들의 소식을 최신순으로 커서 페이지 단위 조회합니다. (전체 조회 무한스크롤)
     *
     * @param bandIds 소식을 조회할 밴드 ID 목록
     * @param cursor  마지막으로 조회한 소식의 postId (첫 페이지면 null)
     * @param size    페이지 크기
     * @return 소식 목록 + 다음 커서 + 다음 페이지 존재 여부
     */
    FollowingBandNewsResponse findFollowingBandNews(List<Long> bandIds, Long cursor, int size);
}
