package com.umc.bscene.domain.post.port;

import com.umc.bscene.domain.post.dto.response.PostCommentListResponse.FanProfileInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface UserPort {

    /**
     * 사용자가 차단한 모든 유저 id를 조회합니다. (라이브 구분 없이 — 댓글 목록에서 차단 유저 댓글 숨김용)
     *
     * @param userId 차단 목록을 조회할 사용자 ID
     * @return 차단한 유저 id 집합
     */
    Set<Long> findBlockedUserIds(Long userId);

    /**
     * 댓글 작성자 표시용 팬 프로필(닉네임/이미지)을 일괄 조회합니다.
     *
     * @param userIds 조회할 사용자 ID 목록
     * @return 사용자 id → 팬 프로필 표시 정보 (팬 프로필이 없는 사용자는 항목 없음)
     */
    Map<Long, FanProfileInfo> findFanProfiles(Collection<Long> userIds);
}
