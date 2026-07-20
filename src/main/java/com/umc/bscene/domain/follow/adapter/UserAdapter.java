package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.follow.entity.Follow;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse.FollowedBandItem;
import com.umc.bscene.domain.user.port.FollowPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 마이페이지의 FollowPort를 follow 도메인이 구현하는 어댑터.
 * - countFollowing                    : 사용자가 팔로우한 밴드 수
 * - countFollowedBandsGroupedByGenre  : 팔로우한 밴드들의 장르 분포 (대표 장르용)
 * - findFollowedBands                 : 팔로우한 밴드 목록 (밴드명 가나다순, 팔로워 수 포함)
 */
@RequiredArgsConstructor
public class UserAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public long countFollowing(Long userId) {
        return followRepository.countByUser_Id(userId);
    }

    @Override
    public Map<Genre, Long> countFollowedBandsGroupedByGenre(Long userId) {
        return followRepository.countGroupedByGenre(userId).stream()
                .collect(Collectors.toMap(row -> (Genre) row[0], row -> (Long) row[1]));
    }

    @Override
    public FollowedBandResponse findFollowedBands(Long userId, int page, int size) {
        Slice<Follow> slice = followRepository.findFollowedBands(userId, PageRequest.of(page, size));

        // 페이지에 담긴 밴드들의 전체 팔로워 수를 IN 쿼리 한 번으로 집계 → 밴드 id별 매핑 (N+1 방지)
        List<Long> bandIds = slice.getContent().stream()
                .map(follow -> follow.getBand().getId())
                .toList();
        Map<Long, Long> followerCounts = bandIds.isEmpty()
                ? Map.of()
                : followRepository.countFollowersByBandIds(bandIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        List<FollowedBandItem> items = slice.getContent().stream()
                .map(follow -> toItem(follow, followerCounts))
                .toList();

        // 총 팔로우 밴드 수(상단 "N팀")는 첫 페이지에서만 조회, 이후 페이지는 생략
        Long totalCount = (page == 0) ? followRepository.countByUser_Id(userId) : null;

        return new FollowedBandResponse(totalCount, items, page, slice.hasNext());
    }

    private FollowedBandItem toItem(Follow follow, Map<Long, Long> followerCounts) {
        Band band = follow.getBand();
        return new FollowedBandItem(
                band.getId(),
                band.getName(),
                band.getGenre(),
                band.getRegion(),
                band.getProfileImageUrl(),
                followerCounts.getOrDefault(band.getId(), 0L)
        );
    }
}
