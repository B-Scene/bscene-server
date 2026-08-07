package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.band.dto.FollowerBlock;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.follow.dto.FollowerRow;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.global.response.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@RequiredArgsConstructor
public class BandAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public Long countFollowersByBandId(Long bandId) {
        return followRepository.countByBand_Id(bandId);
    }

    @Override
    public CursorPage<FollowerBlock> findPagedMyBandFollowers(Long bandId, Long cursor, Integer size) {
        // size + 1 조회로 다음 페이지 존재 여부 판별
        List<FollowerRow> fetched = followRepository.findPagedFollowers(
                bandId,
                cursor,
                PageRequest.ofSize(size + 1)
        );

        boolean hasNext = fetched.size() > size;
        List<FollowerRow> page = hasNext ? fetched.subList(0, size) : fetched;

        List<FollowerBlock> items = page.stream()
                .map(row -> new FollowerBlock(
                        row.userId(),
                        row.fanProfileImageUrl(),
                        row.nickname()
                ))
                .toList();

        Long nextCursor = hasNext ? page.getLast().followId() : null;

        return CursorPage.of(items, nextCursor, hasNext);
    }

    @Override
    public boolean isFollowing(Long userId, Long bandId) {
        return followRepository.existsByBand_IdAndUser_Id(bandId, userId);
    }
}
