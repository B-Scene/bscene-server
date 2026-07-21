package com.umc.bscene.domain.user.adapter;

import com.umc.bscene.domain.post.dto.response.PostCommentListResponse.FanProfileInfo;
import com.umc.bscene.domain.post.port.UserPort;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 게시물 도메인의 UserPort를 user 도메인이 구현하는 어댑터 (차단 목록·댓글 작성자 팬 프로필 조회)
@RequiredArgsConstructor
public class PostAdapter implements UserPort {

    private final UserBlockRepository userBlockRepository;
    private final FanProfileRepository fanProfileRepository;

    @Override
    public Set<Long> findBlockedUserIds(Long userId) {
        return userBlockRepository.findAllBlockedIdsByBlockerId(userId);
    }

    @Override
    public Map<Long, FanProfileInfo> findFanProfiles(Collection<Long> userIds) {
        return fanProfileRepository.findAllByUser_IdIn(userIds).stream()
                .collect(Collectors.toMap(
                        profile -> profile.getUser().getId(),
                        profile -> new FanProfileInfo(profile.getNickname(), profile.getProfileImageUrl())));
    }
}
